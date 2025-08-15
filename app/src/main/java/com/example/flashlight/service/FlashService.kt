package com.example.flashlight.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.flashlight.R

class FlashService : Service() {

    private lateinit var cameraManager: CameraManager
    private lateinit var telephonyManager: TelephonyManager
    private var cameraId: String? = null
    private var isFlashOn = false
    private var isBlinking = false
    private var flashFrequency = 500 // Mặc định 500ms
    private var flashOnCall = false
    private var flashOnSms = false
    private val handler = Handler()

    override fun onCreate() {
        super.onCreate()
        Log.d("FlashService", "Service created")
        // Khởi tạo CameraManager
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager.cameraIdList[0]
            Log.d("FlashService", "Camera ID: $cameraId")
        } catch (e: Exception) {
            Log.e("FlashService", "No flashlight available", e)
            stopSelf()
        }
        // Khởi tạo TelephonyManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("FlashService", "onStartCommand called with action: ${intent?.action}")
        if (intent?.action == "UPDATE_FREQUENCY") {
            flashFrequency = intent.getIntExtra("flashFrequency", 500)
            flashOnCall = intent.getBooleanExtra("flashOnCall", false)
            flashOnSms = intent.getBooleanExtra("flashOnSms", false)
            Log.d(
                "FlashService",
                "Updated flash frequency: $flashFrequency, flashOnCall: $flashOnCall, flashOnSms: $flashOnSms"
            )
            if (isBlinking) {
                handler.removeCallbacks(flashRunnable)
                handler.post(flashRunnable)
            }
            return START_STICKY
        } else if (intent?.action == "SMS_RECEIVED") {
            if (flashOnSms) {
                startFlashBlink()
                // Tắt nháy sau 5 giây
                handler.postDelayed({ stopFlashBlink() }, 5000)
            }
            return START_STICKY
        }

        flashFrequency = intent?.getIntExtra("flashFrequency", 500) ?: 500
        flashOnCall = intent?.getBooleanExtra("flashOnCall", false) ?: false
        flashOnSms = intent?.getBooleanExtra("flashOnSms", false) ?: false
        Log.d(
            "FlashService",
            "Starting service with frequency: $flashFrequency, flashOnCall: $flashOnCall, flashOnSms: $flashOnSms"
        )
        startForegroundService()
        if (flashOnCall) {
            startPhoneStateListener()
        }
        return START_STICKY
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
            .setContentText("Đang chạy tính năng nháy đèn khi có cuộc gọi hoặc SMS")
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .build()

        startForeground(1, notification)
        Log.d("FlashService", "Foreground service started")
    }

    private fun startPhoneStateListener() {
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        Log.d("FlashService", "Phone state listener started")
    }

    private fun stopPhoneStateListener() {
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        Log.d("FlashService", "Phone state listener stopped")
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
                } catch (e: Exception) {
                    Log.e("FlashService", "Error toggling flash", e)
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
        turnOffFlash()
        Log.d("FlashService", "Stopped blinking")
    }

    private fun turnOffFlash() {
        try {
            if (isFlashOn) {
                cameraManager.setTorchMode(cameraId!!, false)
                isFlashOn = false
                Log.d("FlashService", "Flash turned off")
            }
        } catch (e: Exception) {
            Log.e("FlashService", "Error turning off flash", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPhoneStateListener()
        stopFlashBlink()
        Log.d("FlashService", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}