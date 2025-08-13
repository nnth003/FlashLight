package com.example.flashlight.fragment

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.example.flashlight.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

/**
 * A simple [Fragment] subclass.
 * Use the [FlashFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FlashFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var isFlashOn = false

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

        flashButton.setOnClickListener {
            toggleFlashlight(requireContext(), flashButton)
        }
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
}