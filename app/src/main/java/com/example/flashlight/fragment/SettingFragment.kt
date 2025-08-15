package com.example.flashlight.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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

    private lateinit var sharedPreferences: SharedPreferences
    private var isFlashOnCallEnabled = false
    private var isFlashOnSmsEnabled = false
    private var isExitConfirmationEnabled = false
    private var flashFrequency = 500 // Default 500ms

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = requireContext().getSharedPreferences("FlashlightPrefs", Context.MODE_PRIVATE)
        isFlashOnCallEnabled = sharedPreferences.getBoolean("flashOnCall", false)
        isFlashOnSmsEnabled = sharedPreferences.getBoolean("flashOnSms", false)
        isExitConfirmationEnabled = sharedPreferences.getBoolean("exitConfirmation", false)
        flashFrequency = sharedPreferences.getInt("flashFrequency", 500)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val switchFlashOnCall = view.findViewById<SwitchCompat>(R.id.switch_flash_on_call)
        val switchFlashOnSms = view.findViewById<SwitchCompat>(R.id.switch_flash_on_sms)
//        val switchExitConfirmation = view.findViewById<SwitchCompat>(R.id.switch_exit_confirmation)
        val seekBarFlashFrequency = view.findViewById<SeekBar>(R.id.seekbar_flash_frequency)
        val textFlashFrequency = view.findViewById<TextView>(R.id.text_flash_frequency)

        switchFlashOnCall.isChecked = isFlashOnCallEnabled
        switchFlashOnSms.isChecked = isFlashOnSmsEnabled
//        switchExitConfirmation.isChecked = isExitConfirmationEnabled
        seekBarFlashFrequency.progress = 1000 - flashFrequency
        textFlashFrequency.text = "$flashFrequency ms"

        // Check permissions and MIUI settings on startup
        if (!hasPermissions()) {
            requestPermissions()
        } else if (isFlashOnCallEnabled || isFlashOnSmsEnabled || sharedPreferences.getBoolean("isFlashOn", false)) {
            promptMiuiSettings()
            startFlashService()
        }

        switchFlashOnCall.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !hasPhoneStatePermission()) {
                switchFlashOnCall.isChecked = false
                Toast.makeText(context, "Cần quyền READ_PHONE_STATE để nháy đèn khi có cuộc gọi!", Toast.LENGTH_SHORT).show()
                requestPermissions()
                return@setOnCheckedChangeListener
            }
            isFlashOnCallEnabled = isChecked
            with(sharedPreferences.edit()) {
                putBoolean("flashOnCall", isFlashOnCallEnabled)
                apply()
            }
            if (isChecked) promptMiuiSettings()
            updateFlashService()
        }

        switchFlashOnSms.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !hasSmsPermission()) {
                switchFlashOnSms.isChecked = false
                Toast.makeText(context, "Cần quyền RECEIVE_SMS để nháy đèn khi có tin nhắn!", Toast.LENGTH_SHORT).show()
                requestPermissions()
                return@setOnCheckedChangeListener
            }
            isFlashOnSmsEnabled = isChecked
            with(sharedPreferences.edit()) {
                putBoolean("flashOnSms", isFlashOnSmsEnabled)
                apply()
            }
            if (isChecked) promptMiuiSettings()
            updateFlashService()
        }

//        switchExitConfirmation.setOnCheckedChangeListener { _, isChecked ->
//            isExitConfirmationEnabled = isChecked
//            with(sharedPreferences.edit()) {
//                putBoolean("exitConfirmation", isExitConfirmationEnabled)
//                apply()
//            }
//        }

        seekBarFlashFrequency.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                flashFrequency = 1000 - progress
                textFlashFrequency.text = "$flashFrequency ms"
                with(sharedPreferences.edit()) {
                    putInt("flashFrequency", flashFrequency)
                    apply()
                }
                updateFlashService()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun hasPermissions(): Boolean {
        return hasPhoneStatePermission() && hasCameraPermission() && hasSmsPermission()
    }

    private fun hasPhoneStatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
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

    private fun promptMiuiSettings() {
        if (Build.MANUFACTURER.equals("xiaomi", ignoreCase = true)) {
            val sharedPreferences = requireContext().getSharedPreferences("FlashlightPrefs", Context.MODE_PRIVATE)
            val hasPrompted = sharedPreferences.getBoolean("hasPromptedMiuiSettings", false)
            if (!hasPrompted) {
                Toast.makeText(
                    context,
                    "Vui lòng bật Tự động khởi động và tắt Tối ưu hóa pin cho ứng dụng để đảm bảo hoạt động nền!",
                    Toast.LENGTH_LONG
                ).show()
                try {
                    // Prompt for auto-start
                    val autoStartIntent = Intent().apply {
                        setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                    }
                    startActivity(autoStartIntent)
                } catch (e: Exception) {
                    Log.e("SettingFragment", "Failed to open MIUI auto-start settings", e)
                }
                try {
                    // Prompt for battery optimization
                    val batteryIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${requireContext().packageName}")
                    }
                    startActivity(batteryIntent)
                } catch (e: Exception) {
                    Log.e("SettingFragment", "Failed to open battery optimization settings", e)
                }
                with(sharedPreferences.edit()) {
                    putBoolean("hasPromptedMiuiSettings", true)
                    apply()
                }
            }
        }
    }

    private fun startFlashService() {
        if (!hasPermissions()) {
            Log.w("SettingFragment", "Cannot start FlashService: missing permissions")
            return
        }
        Log.d("SettingFragment", "Starting FlashService with frequency: $flashFrequency")
        val intent = Intent(requireContext(), FlashService::class.java).apply {
            putExtra("flashFrequency", flashFrequency)
            putExtra("flashOnCall", isFlashOnCallEnabled)
            putExtra("flashOnSms", isFlashOnSmsEnabled)
            putExtra("isFlashOn", sharedPreferences.getBoolean("isFlashOn", false))
        }
        try {
            ContextCompat.startForegroundService(requireContext(), intent)
        } catch (e: Exception) {
            Log.e("SettingFragment", "Failed to start FlashService", e)
            Toast.makeText(context, "Lỗi khi khởi động dịch vụ đèn pin", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopFlashService() {
        Log.d("SettingFragment", "Stopping FlashService")
        val intent = Intent(requireContext(), FlashService::class.java)
        try {
            requireContext().stopService(intent)
        } catch (e: Exception) {
            Log.e("SettingFragment", "Failed to stop FlashService", e)
        }
    }

    private fun updateFlashService() {
        if (!hasPermissions()) {
            Log.w("SettingFragment", "Cannot update FlashService: missing permissions")
            return
        }
        val isFlashOn = sharedPreferences.getBoolean("isFlashOn", false)
        if (!isFlashOnCallEnabled && !isFlashOnSmsEnabled && !isFlashOn) {
            Log.d("SettingFragment", "No features enabled, stopping FlashService")
            stopFlashService()
            return
        }
        Log.d("SettingFragment", "Updating FlashService with frequency: $flashFrequency, flashOnCall: $isFlashOnCallEnabled, flashOnSms: $isFlashOnSmsEnabled, isFlashOn: $isFlashOn")
        val intent = Intent(requireContext(), FlashService::class.java).apply {
            action = "UPDATE_FREQUENCY"
            putExtra("flashFrequency", flashFrequency)
            putExtra("flashOnCall", isFlashOnCallEnabled)
            putExtra("flashOnSms", isFlashOnSmsEnabled)
            putExtra("isFlashOn", isFlashOn)
        }
        try {
            ContextCompat.startForegroundService(requireContext(), intent)
        } catch (e: Exception) {
            Log.e("SettingFragment", "Failed to update FlashService", e)
            Toast.makeText(context, "Lỗi khi cập nhật dịch vụ đèn pin", Toast.LENGTH_SHORT).show()
        }
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
                if (isFlashOnCallEnabled || isFlashOnSmsEnabled || sharedPreferences.getBoolean("isFlashOn", false)) {
                    promptMiuiSettings()
                    startFlashService()
                }
            } else {
                Toast.makeText(context, "Cần cấp quyền để sử dụng tính năng!", Toast.LENGTH_SHORT).show()
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