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
import androidx.lifecycle.ViewModelProvider
import com.example.flashlight.R
import com.example.flashlight.viewmodel.FlashViewModel

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
    private var isBlinking = false
    private var sosHandler: Handler? = null
    private var sosRunnable: Runnable? = null
    private var blinkHandler: Handler? = null
    private var blinkRunnable: Runnable? = null

    private lateinit var viewModel: FlashViewModel


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


        viewModel = ViewModelProvider(requireActivity())[FlashViewModel::class.java]

        viewModel.isFlashOn.observe(viewLifecycleOwner) { isOn ->
            flashButton.setImageResource(
                if (isOn) R.drawable.outline_flashlight_off_96
                else R.drawable.outline_flashlight_on_96
            )
            if (isOn) {
                flashButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green))
            } else {
                flashButton.clearColorFilter()
            }
        }

        viewModel.isSosActive.observe(viewLifecycleOwner) { isActive ->
            if (isActive) {
                sosButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
            } else {
                sosButton.clearColorFilter()
            }
        }

        viewModel.isBlinking.observe(viewLifecycleOwner) { isBlinking ->
            if (isBlinking) {
                loadingButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.yellow))
                speedSeekBar.visibility = View.VISIBLE
            } else {
                loadingButton.clearColorFilter()
                speedSeekBar.visibility = View.GONE
            }
        }

        // Các sự kiện click
        flashButton.setOnClickListener {
            if (viewModel.isSosActive.value == true) viewModel.stopSos()
            if (viewModel.isBlinking.value == true) viewModel.stopBlinking()
            viewModel.toggleFlash()
        }

        sosButton.setOnClickListener {
            if (viewModel.isBlinking.value == true) viewModel.stopBlinking()
            if (viewModel.isFlashOn.value == true) viewModel.turnFlashOff()
            if (viewModel.isSosActive.value == true) viewModel.stopSos()
            else viewModel.startSos()
        }

        loadingButton.setOnClickListener {
            if (viewModel.isSosActive.value == true) viewModel.stopSos()
            if (viewModel.isFlashOn.value == true) viewModel.turnFlashOff()
            if (viewModel.isBlinking.value == true) viewModel.stopBlinking()
            else viewModel.startBlinking()
        }

        speedSeekBar.max = 10
        speedSeekBar.progress = 5
        speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val minDelay = 50L
                val maxDelay = 500L
                val ratio = 1.0 - (progress / 10.0)
                viewModel.delayUnit = (minDelay + (maxDelay - minDelay) * ratio).toLong()

                if (viewModel.isBlinking.value == true) {
                    viewModel.stopBlinking()
                    viewModel.startBlinking()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
}