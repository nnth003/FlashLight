package com.example.flashlight.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class FlashViewModel(application: Application) : AndroidViewModel(application) {
    private val cameraManager = application.getSystemService(CameraManager::class.java)
    private val cameraId = cameraManager.cameraIdList[0]

    private val _isFlashOn = MutableLiveData(false)
    val isFlashOn: LiveData<Boolean> = _isFlashOn

    private val _isSosActive = MutableLiveData(false)
    val isSosActive: LiveData<Boolean> = _isSosActive

    private val _isBlinking = MutableLiveData(false)
    val isBlinking: LiveData<Boolean> = _isBlinking

    private val _isListening = MutableLiveData(false)
    val isListening: LiveData<Boolean> = _isListening

    var delayUnit: Long = 200L

    private var sosHandler: Handler? = null
    private var sosRunnable: Runnable? = null

    private var blinkHandler: Handler? = null
    private var blinkRunnable: Runnable? = null

    private var audioRecord: AudioRecord? = null
    private var isAudioRunning = false

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
    fun toggleFlash() {
        val newState = !(isFlashOn.value ?: false)
        _isFlashOn.value = newState
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, newState)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun turnFlashOff() {
        _isFlashOn.value = false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startSos() {
        if (_isSosActive.value == true) return

        val sosPattern = listOf(
            true, false, true, false, true, false,       // S
            true, true, true, false,                     // O
            true, false, true, false, true, false        // S
        )
        var index = 0
        val delay = delayUnit

        sosHandler = Handler(Looper.getMainLooper())
        _isSosActive.value = true

        sosRunnable = object : Runnable {
            override fun run() {
                if (_isSosActive.value != true) return
                val turnOn = sosPattern.getOrNull(index) ?: false
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        cameraManager.setTorchMode(cameraId, turnOn)
                    }
                } catch (e: Exception) {
                    stopSos()
                    return
                }

                index++
                if (index >= sosPattern.size) {
                    index = 0
                    sosHandler?.postDelayed(this, delay * 3)
                } else {
                    sosHandler?.postDelayed(this, delay)
                }
            }
        }
        sosHandler?.post(sosRunnable!!)
    }

    fun stopSos() {
        _isSosActive.value = false
        sosHandler?.removeCallbacks(sosRunnable ?: return)
        sosHandler = null
        sosRunnable = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startBlinking() {
        if (_isBlinking.value == true) return

        blinkHandler = Handler(Looper.getMainLooper())
        var isOn = false
        blinkRunnable = object : Runnable {
            override fun run() {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        cameraManager.setTorchMode(cameraId, isOn)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                isOn = !isOn
                if (_isBlinking.value == true) {
                    blinkHandler?.postDelayed(this, delayUnit)
                }
            }
        }
        _isBlinking.value = true
        blinkHandler?.post(blinkRunnable!!)
    }

    fun stopBlinking() {
        _isBlinking.value = false
        blinkHandler?.removeCallbacks(blinkRunnable!!)
        blinkHandler = null
        blinkRunnable = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startListeningToSound() {
        if (_isListening.value == true) return

        if (!hasPermission(android.Manifest.permission.RECORD_AUDIO) ||
            !hasPermission(android.Manifest.permission.CAMERA)
        ) {
            _isListening.postValue(false)
            return
        }

        val bufferSize = AudioRecord.getMinBufferSize(
            8000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                8000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            _isListening.postValue(false)
            return
        }


        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            _isListening.postValue(false)
            audioRecord?.release()
            audioRecord = null
            return
        }

        audioRecord?.startRecording()
        isAudioRunning = true
        _isListening.value = true

        Thread {
            val buffer = ShortArray(bufferSize)
            while (isAudioRunning) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val max = buffer.maxOrNull()?.toInt() ?: 0
                    if (max > 2000) {  // Ngưỡng âm thanh
                        flashOnce()
                        Thread.sleep(150) // Giảm tần suất nháy
                    }
                }
            }
        }.start()
    }

    fun stopListeningToSound() {
        isAudioRunning = false
        _isListening.value = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun flashOnce() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, true)
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        cameraManager.setTorchMode(cameraId, false)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, 100)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}