package com.example.flashlight.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.flashlight.R

class FlashService : Service() {

    private lateinit var cameraManager: CameraManager
    private lateinit var telephonyManager: TelephonyManager
    private var cameraId: String? = null
    private var isFlashOn = false
    private var isBlinking = false
    private var flashFrequency = 500 // Default 500ms
    private var flashOnCall = false
    private var flashOnSms = false
    private var manualFlashOn = false
    private val handler = Handler()

    override fun onCreate() {
        super.onCreate()
        Log.d("FlashService", "Service created")
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager.cameraIdList.firstOrNull()
            if (cameraId == null) {
                Log.e("FlashService", "No camera available for flashlight")
                stopSelf()
                return
            }
            Log.d("FlashService", "Camera ID: $cameraId")
        } catch (e: CameraAccessException) {
            Log.e("FlashService", "Camera access error during initialization", e)
            stopSelf()
            return
        } catch (e: Exception) {
            Log.e("FlashService", "Unexpected error initializing camera", e)
            stopSelf()
            return
        }
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("FlashService", "onStartCommand called with action: ${intent?.action}")
        if (cameraId == null) {
            Log.e("FlashService", "Camera ID is null, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        // Start foreground service immediately to avoid ForegroundServiceDidNotStartInTimeException
        startForegroundService()

        when (intent?.action) {
            "UPDATE_FREQUENCY" -> {
                flashFrequency = intent.getIntExtra("flashFrequency", 500)
                flashOnCall = intent.getBooleanExtra("flashOnCall", false)
                flashOnSms = intent.getBooleanExtra("flashOnSms", false)
                manualFlashOn = intent.getBooleanExtra("isFlashOn", false)
                Log.d("FlashService", "Updated: flashFrequency=$flashFrequency, flashOnCall=$flashOnCall, flashOnSms=$flashOnSms, manualFlashOn=$manualFlashOn")
                if (isBlinking) {
                    handler.removeCallbacks(flashRunnable)
                    handler.post(flashRunnable)
                }
                if (manualFlashOn && !isBlinking) {
                    turnOnFlash()
                } else if (!manualFlashOn && !isBlinking) {
                    turnOffFlash()
                }
                if (!flashOnCall && !flashOnSms && !manualFlashOn) {
                    Log.d("FlashService", "No features enabled, stopping service")
                    stopSelf()
                }
                return START_STICKY
            }
            "SMS_RECEIVED" -> {
                if (flashOnSms && hasSmsPermission()) {
                    startFlashBlink()
                    handler.postDelayed({ stopFlashBlink() }, 5000)
                } else {
                    Log.w("FlashService", "SMS flashing disabled or missing RECEIVE_SMS permission")
                }
                return START_STICKY
            }
            else -> {
                flashFrequency = intent?.getIntExtra("flashFrequency", 500) ?: 500
                flashOnCall = intent?.getBooleanExtra("flashOnCall", false) ?: false
                flashOnSms = intent?.getBooleanExtra("flashOnSms", false) ?: false
                manualFlashOn = intent?.getBooleanExtra("isFlashOn", false) ?: false
                Log.d("FlashService", "Starting service: flashFrequency=$flashFrequency, flashOnCall=$flashOnCall, flashOnSms=$flashOnSms, manualFlashOn=$manualFlashOn")
                if (flashOnCall && hasPhoneStatePermission()) {
                    startPhoneStateListener()
                }
                if (manualFlashOn && !isBlinking) {
                    turnOnFlash()
                }
                if (!flashOnCall && !flashOnSms && !manualFlashOn) {
                    Log.d("FlashService", "No features enabled, stopping service")
                    stopSelf()
                }
                return START_STICKY
            }
        }
    }

    private fun startForegroundService() {
        val channelId = "FlashServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Flash Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Flashlight Service")
            .setContentText("Đang chạy tính năng đèn pin hoặc nháy đèn khi có cuộc gọi/SMS")
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .build()

        try {
            startForeground(1, notification)
            Log.d("FlashService", "Foreground service started")
        } catch (e: Exception) {
            Log.e("FlashService", "Failed to start foreground service", e)
            stopSelf()
        }
    }

    private fun startPhoneStateListener() {
        try {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            Log.d("FlashService", "Phone state listener started")
        } catch (e: SecurityException) {
            Log.e("FlashService", "Missing READ_PHONE_STATE permission", e)
        } catch (e: Exception) {
            Log.e("FlashService", "Error starting phone state listener", e)
        }
    }

    private fun stopPhoneStateListener() {
        try {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
            Log.d("FlashService", "Phone state listener stopped")
        } catch (e: Exception) {
            Log.e("FlashService", "Error stopping phone state listener", e)
        }
    }

    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)
            Log.d("FlashService", "Call state changed: $state")
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> if (flashOnCall) startFlashBlink()
                TelephonyManager.CALL_STATE_IDLE, TelephonyManager.CALL_STATE_OFFHOOK -> stopFlashBlink()
            }
        }
    }

    private val flashRunnable = object : Runnable {
        override fun run() {
            if (isBlinking) {
                try {
                    cameraManager.setTorchMode(cameraId!!, !isFlashOn)
                    isFlashOn = !isFlashOn
                    Log.d("FlashService", "Flash toggled: $isFlashOn")
                    handler.postDelayed(this, flashFrequency.toLong())
                } catch (e: CameraAccessException) {
                    Log.e("FlashService", "Camera access error while toggling flash", e)
                } catch (e: Exception) {
                    Log.e("FlashService", "Unexpected error toggling flash", e)
                }
            }
        }
    }

    private fun startFlashBlink() {
        if (!isBlinking) {
            isBlinking = true
            handler.post(flashRunnable)
            Log.d("FlashService", "Started blinking with frequency: $flashFrequency")
        }
    }

    private fun stopFlashBlink() {
        isBlinking = false
        handler.removeCallbacks(flashRunnable)
        if (manualFlashOn) {
            turnOnFlash()
        } else {
            turnOffFlash()
        }
        Log.d("FlashService", "Stopped blinking")
    }

    private fun turnOnFlash() {
        try {
            if (!isFlashOn && !isBlinking) {
                cameraManager.setTorchMode(cameraId!!, true)
                isFlashOn = true
                with(getSharedPreferences("FlashlightPrefs", MODE_PRIVATE).edit()) {
                    putBoolean("isFlashOn", true)
                    apply()
                }
                Log.d("FlashService", "Flash turned on")
            }
        } catch (e: CameraAccessException) {
            Log.e("FlashService", "Camera access error turning on flash", e)
        } catch (e: Exception) {
            Log.e("FlashService", "Unexpected error turning on flash", e)
        }
    }

    private fun turnOffFlash() {
        try {
            if (isFlashOn && !isBlinking) {
                cameraManager.setTorchMode(cameraId!!, false)
                isFlashOn = false
                with(getSharedPreferences("FlashlightPrefs", MODE_PRIVATE).edit()) {
                    putBoolean("isFlashOn", false)
                    apply()
                }
                Log.d("FlashService", "Flash turned off")
            }
        } catch (e: CameraAccessException) {
            Log.e("FlashService", "Camera access error turning off flash", e)
        } catch (e: Exception) {
            Log.e("FlashService", "Unexpected error turning off flash", e)
        }
    }

    private fun hasPhoneStatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPhoneStateListener()
        stopFlashBlink()
        if (manualFlashOn) {
            try {
                cameraManager.setTorchMode(cameraId!!, true)
                Log.d("FlashService", "Service destroyed, manualFlashOn=true, keeping flash on")
            } catch (e: Exception) {
                Log.e("FlashService", "Error keeping flash on during destroy", e)
            }
        } else {
            Log.d("FlashService", "Service destroyed, no manual flash")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}