package com.example.flashlight.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.flashlight.R
import com.example.flashlight.service.FlashService

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


/**
 * A simple [Fragment] subclass.
 * Use the [SettingFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private lateinit var sharedPreferences: SharedPreferences
    private var isFlashOnCallEnabled = false
    private var isFlashOnSmsEnabled = false
    private var isExitConfirmationEnabled = false
    private var flashFrequency = 500 // Mặc định 500ms


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Khởi tạo SharedPreferences
        sharedPreferences =
            requireContext().getSharedPreferences("FlashlightPrefs", Context.MODE_PRIVATE)
        // Khôi phục trạng thái từ SharedPreferences
        isFlashOnCallEnabled = sharedPreferences.getBoolean("flashOnCall", false)
        isFlashOnSmsEnabled = sharedPreferences.getBoolean("flashOnSms", false)
        isExitConfirmationEnabled = sharedPreferences.getBoolean("exitConfirmation", false)
        flashFrequency = sharedPreferences.getInt("flashFrequency", 500)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val switchFlashOnCall = view.findViewById<SwitchCompat>(R.id.switch_flash_on_call)
        val switchFlashOnSms = view.findViewById<SwitchCompat>(R.id.switch_flash_on_sms)
        val seekBarFlashFrequency = view.findViewById<SeekBar>(R.id.seekbar_flash_frequency)
        val textFlashFrequency = view.findViewById<TextView>(R.id.text_flash_frequency)

        // Khôi phục trạng thái switch
        switchFlashOnCall.isChecked = isFlashOnCallEnabled
        switchFlashOnSms.isChecked = isFlashOnSmsEnabled

        // Khởi tạo SeekBar và TextView
        seekBarFlashFrequency.progress =
            1000 - flashFrequency // Ánh xạ ngược: progress 0 -> 1000ms, progress 800 -> 200ms
        textFlashFrequency.text = "$flashFrequency ms"

        // Kiểm tra và yêu cầu quyền
        if (!hasPermissions()) {
            requestPermissions()
        } else if (isFlashOnCallEnabled || isFlashOnSmsEnabled) {
            startFlashService()
        }

        // Lắng nghe trạng thái switch cho cuộc gọi
        switchFlashOnCall.setOnCheckedChangeListener { _, isChecked ->
            isFlashOnCallEnabled = isChecked
            with(sharedPreferences.edit()) {
                putBoolean("flashOnCall", isFlashOnCallEnabled)
                apply()
            }
            if (isChecked && hasPermissions()) {
                startFlashService()
            } else if (!isFlashOnCallEnabled && !isFlashOnSmsEnabled) {
                stopFlashService()
            }
        }

        // Lắng nghe trạng thái switch cho SMS
        switchFlashOnSms.setOnCheckedChangeListener { _, isChecked ->
            isFlashOnSmsEnabled = isChecked
            with(sharedPreferences.edit()) {
                putBoolean("flashOnSms", isFlashOnSmsEnabled)
                apply()
            }
            if (isChecked && hasPermissions()) {
                startFlashService()
            } else if (!isFlashOnCallEnabled && !isFlashOnSmsEnabled) {
                stopFlashService()
            }
        }
        // Lắng nghe trạng thái switch cho xác nhận thoát
//        switchExitConfirmation.setOnCheckedChangeListener { _, isChecked ->
//            isExitConfirmationEnabled = isChecked
//            with(sharedPreferences.edit()) {
//                putBoolean("exitConfirmation", isExitConfirmationEnabled)
//                apply()
//            }
//        }
        // Lắng nghe SeekBar để điều chỉnh tần suất
        seekBarFlashFrequency.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                flashFrequency = 1000 - progress
                textFlashFrequency.text = "$flashFrequency ms"
                with(sharedPreferences.edit()) {
                    putInt("flashFrequency", flashFrequency)
                    apply()
                }
                if (isFlashOnCallEnabled || isFlashOnSmsEnabled) {
                    updateFlashService()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.RECEIVE_SMS
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECEIVE_SMS
            ),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun startFlashService() {
        Log.d("SettingFragment", "Starting FlashService with frequency: $flashFrequency")
        val intent = Intent(requireContext(), FlashService::class.java).apply {
            putExtra("flashFrequency", flashFrequency)
            putExtra("flashOnCall", isFlashOnCallEnabled)
            putExtra("flashOnSms", isFlashOnSmsEnabled)
        }
        ContextCompat.startForegroundService(requireContext(), intent)
    }

    private fun stopFlashService() {
        Log.d("SettingFragment", "Stopping FlashService")
        val intent = Intent(requireContext(), FlashService::class.java)
        requireContext().stopService(intent)
    }

    private fun updateFlashService() {
        Log.d("SettingFragment", "Updating FlashService with frequency: $flashFrequency")
        val intent = Intent(requireContext(), FlashService::class.java).apply {
            action = "UPDATE_FREQUENCY"
            putExtra("flashFrequency", flashFrequency)
            putExtra("flashOnCall", isFlashOnCallEnabled)
            putExtra("flashOnSms", isFlashOnSmsEnabled)
        }
        requireContext().startService(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(context, "Quyền được cấp!", Toast.LENGTH_SHORT).show()
                if (isFlashOnCallEnabled || isFlashOnSmsEnabled) {
                    startFlashService()
                }
            } else {
                Toast.makeText(context, "Cần cấp quyền để sử dụng tính năng!", Toast.LENGTH_SHORT)
                    .show()
                view?.findViewById<SwitchCompat>(R.id.switch_flash_on_call)?.isChecked = false
                view?.findViewById<SwitchCompat>(R.id.switch_flash_on_sms)?.isChecked = false
                isFlashOnCallEnabled = false
                isFlashOnSmsEnabled = false
                with(sharedPreferences.edit()) {
                    putBoolean("flashOnCall", false)
                    putBoolean("flashOnSms", false)
                    apply()
                }
                stopFlashService()
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}