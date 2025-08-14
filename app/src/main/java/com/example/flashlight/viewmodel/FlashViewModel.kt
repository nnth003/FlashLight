package com.example.flashlight.viewmodel

import android.app.Application
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
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

    var delayUnit: Long = 200L

    private var sosHandler: Handler? = null
    private var sosRunnable: Runnable? = null

    private var blinkHandler: Handler? = null
    private var blinkRunnable: Runnable? = null

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
}