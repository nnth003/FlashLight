package com.example.flashlight.fragment

import android.content.Context
import android.graphics.PorterDuff
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import com.example.flashlight.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private var delayUnit = 200L // đơn vị nháy đèn mặc định

/**
 * A simple [Fragment] subclass.
 * Use the [FlashFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FlashFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var isFlashOn = false
    private var isSosActive = false
    private var sosHandler: Handler? = null
    private var sosRunnable: Runnable? = null
    private var blinkHandler: Handler? = null
    private var blinkRunnable: Runnable? = null
    private var isBlinking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_flash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val flashButton = view.findViewById<ImageView>(R.id.flashButton)
        val sosButton = view.findViewById<ImageView>(R.id.sosButton)
        val loadingButton = view.findViewById<ImageView>(R.id.loadingButton)
        val speedSeekBar = view.findViewById<SeekBar>(R.id.speedSeekBar)

        flashButton.setOnClickListener {
            if (isSosActive) {
                stopSosSignal(requireContext())
                sosButton.setImageResource(R.drawable.sos_96)
                sosButton.clearColorFilter()
            }
            if (isBlinking) {
                stopBlinking(requireContext())
                loadingButton.clearColorFilter()
                speedSeekBar.visibility = View.GONE
            }
            toggleFlashlight(requireContext(), flashButton)
            if (isFlashOn) {
                flashButton.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.green),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                flashButton.clearColorFilter()
            }
        }

        sosButton.setOnClickListener {
            if (isFlashOn) {
                toggleFlashlight(requireContext(), flashButton)
                flashButton.clearColorFilter()
            }
            if (isBlinking) {
                stopBlinking(requireContext())
                loadingButton.clearColorFilter()
                speedSeekBar.visibility = View.GONE
            }
            if (!isSosActive) {
                startSosSignal(requireContext())
//                sosButton.setImageResource(R.drawable.sos_96) // icon stop (nếu có)
                sosButton.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.red),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                stopSosSignal(requireContext())
//                sosButton.setImageResource(R.drawable.sos_96)
                sosButton.clearColorFilter()
            }
        }
        loadingButton.setOnClickListener {
            if (isSosActive) {
                stopSosSignal(requireContext())
                sosButton.clearColorFilter()
                isSosActive = false
            }
            if (isFlashOn) {
                toggleFlashlight(requireContext(), flashButton)
                flashButton.clearColorFilter()
                isFlashOn = false
            }
            if (!isBlinking) {
                startBlinking(requireContext(), delayUnit)
                loadingButton.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.yellow), // màu báo đang nháy
                    PorterDuff.Mode.SRC_IN
                )
                speedSeekBar.visibility = View.VISIBLE
                isBlinking = true
            } else {
                stopBlinking(requireContext())
                loadingButton.clearColorFilter()
                speedSeekBar.visibility = View.GONE
                isBlinking = false
            }
        }
        speedSeekBar.max = 10
        speedSeekBar.progress = 5 // mặc định

        speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val minDelay = 50L
                val maxDelay = 500L
                val ratio = 1.0 - (progress / 10.0)
                delayUnit = (minDelay + (maxDelay - minDelay) * ratio).toLong()

                // Nếu đang nháy thì update tốc độ
                if (isBlinking) {
                    stopBlinking(requireContext())
                    startBlinking(requireContext(), delayUnit)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun toggleFlashlight(context: Context, flashButton: ImageView) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0]

        isFlashOn = !isFlashOn

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, isFlashOn)
            }
//            flashButton.setImageResource(
//                if (isFlashOn) R.drawable.outline_flashlight_on_96
//                else R.drawable.outline_flashlight_on_96
//            )
            if (isFlashOn) {
                flashButton.setImageResource(R.drawable.outline_flashlight_off_96) // icon tắt đèn
            } else {
                flashButton.setImageResource(R.drawable.outline_flashlight_on_96) // icon bật đèn
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startSosSignal(context: Context) {
        val delay = delayUnit

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return
        val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return

        val sosPattern = listOf(
            true, false, true, false, true, false,       // S: dot-dot-dot
            true, true, true, false,                     // O: dash-dash-dash
            true, false, true, false, true, false        // S: dot-dot-dot
        )

        val delayUnit = 200L
        var index = 0

        sosHandler = Handler(Looper.getMainLooper())
        isSosActive = true

        sosRunnable = object : Runnable {
            override fun run() {
                if (!isSosActive) return

                val turnOn = sosPattern.getOrNull(index) ?: false
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        cameraManager.setTorchMode(cameraId, turnOn)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    stopSosSignal(context)
                    return
                }

                val delay = delayUnit
                index++

                if (index >= sosPattern.size) {
                    index = 0
                    sosHandler?.postDelayed(this, delayUnit * 3) // nghỉ giữa các vòng
                } else {
                    sosHandler?.postDelayed(this, delay)
                }
            }
        }

        sosRunnable?.let { sosHandler?.post(it) }
    }

    private fun stopSosSignal(context: Context) {
        isSosActive = false
        sosHandler?.removeCallbacks(sosRunnable ?: return)
        sosHandler = null
        sosRunnable = null

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return
        val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Bật nháy đèn lặp lại liên tục với khoảng delay cho trước.
     * @param context Context để lấy CameraManager.
     * @param delayMillis Thời gian (ms) giữa lần bật/tắt đèn.
     */
    fun startBlinking(context: Context, delayMillis: Long) {
        if (isBlinking) return // nếu đang nháy thì không bắt đầu lại

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0]
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

                isOn = !isOn // đảo trạng thái đèn
                if (isBlinking) {
                    blinkHandler?.postDelayed(this, delayMillis)
                }
            }
        }

        isBlinking = true
        blinkRunnable?.let { blinkHandler?.post(it) }
    }

    /**
     * Dừng nháy đèn liên tục.
     * @param context Context để lấy CameraManager.
     */
    fun stopBlinking(context: Context) {
        if (!isBlinking) return

        isBlinking = false
        blinkHandler?.removeCallbacks(blinkRunnable!!)
        blinkHandler = null
        blinkRunnable = null

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0]
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}