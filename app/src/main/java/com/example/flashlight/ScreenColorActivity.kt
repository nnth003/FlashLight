package com.example.flashlight

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import yuku.ambilwarna.AmbilWarnaDialog

class ScreenColorActivity : AppCompatActivity() {

    private lateinit var rootLayout: View
    private var currentColor: Int = Color.RED
    private lateinit var brightnessSeekBar: SeekBar
    private var hasWriteSettingsPermission = false
    private var writeSettingsDialog: AlertDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_screen_color)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.screenRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

//        if (!Settings.System.canWrite(this)) {
//            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
//            intent.data = Uri.parse("package:$packageName")
//            startActivity(intent)
//        }

        rootLayout = findViewById(R.id.screenRoot)
        val changeColorButton = findViewById<Button>(R.id.changeColorButton)
        val backButton = findViewById<ImageButton>(R.id.backButton)

        rootLayout.setBackgroundColor(currentColor)

        changeColorButton.setOnClickListener {
            openColorPicker()
        }

        backButton.setOnClickListener {
            finish() // Quay lại màn hình trước
        }

        brightnessSeekBar = findViewById(R.id.brightnessSeekBar)
        brightnessSeekBar.max = 255
        updatePermissionStatus()

        // Đặt giá trị ban đầu
        val currentBrightness = Settings.System.getInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            125
        )

        brightnessSeekBar.progress = currentBrightness

        brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                if (!fromUser) return

                if (hasWriteSettingsPermission) {
                    setScreenBrightness(progress)
                } else {
                    showWriteSettingsDialog()
                    Toast.makeText(
                        this@ScreenColorActivity,
                        "Vui lòng cấp quyền để thay đổi độ sáng màn hình",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Reset về độ sáng hiện tại
                    val currentBrightness = Settings.System.getInt(
                        contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        125
                    )
                    brightnessSeekBar.progress = currentBrightness
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        hasWriteSettingsPermission = Settings.System.canWrite(this)
        brightnessSeekBar.isEnabled = true // Luôn enable để bắt sự kiện kéo
    }
//    private fun checkWriteSettingsPermission() {
//        if (!Settings.System.canWrite(this)) {
//            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
//                data = Uri.parse("package:$packageName")
//            }
//            startActivity(intent)
//            Toast.makeText(
//                this,
//                "Vui lòng cấp quyền để thay đổi độ sáng màn hình",
//                Toast.LENGTH_LONG
//            ).show()
//        } else {
//            // Đã có quyền → có thể chỉnh độ sáng nếu muốn
//        }
//    }

    private fun checkAndRequestWriteSettingsPermission() {
        if (!Settings.System.canWrite(this)) {
            showWriteSettingsDialog()
        }
    }

    private fun showWriteSettingsDialog() {
        if (writeSettingsDialog?.isShowing == true) return

        writeSettingsDialog = AlertDialog.Builder(this)
            .setTitle("Yêu cầu quyền")
            .setMessage("Ứng dụng cần quyền thay đổi cài đặt hệ thống để điều chỉnh độ sáng màn hình. Bạn có muốn cấp quyền không?")
            .setPositiveButton("Đồng ý") { _, _ ->
                // Mở trang cấp quyền WRITE_SETTINGS
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                writeSettingsDialog = null
            }
            .setNegativeButton("Không") { dialog, _ ->
                dialog.dismiss()
                writeSettingsDialog = null
            }
            .setOnDismissListener {
                writeSettingsDialog = null
            }
            .show()
    }

    private fun openColorPicker() {
        val colorPicked =
            AmbilWarnaDialog(
                this,
                currentColor,
                object : AmbilWarnaDialog.OnAmbilWarnaListener {
                    override fun onCancel(dialog: AmbilWarnaDialog?) {

                    }

                    override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                        currentColor = color
                        rootLayout.setBackgroundColor(color)
                    }
                }
            )
        colorPicked.show()
    }

    private fun setScreenBrightness(brightness: Int) {
        // Gán độ sáng cho cửa sổ hiện tại
        val layoutParams = window.attributes
        layoutParams.screenBrightness = brightness / 255f
        window.attributes = layoutParams

        // Nếu có quyền, gán độ sáng cho hệ thống (tùy chọn)
        if (Settings.System.canWrite(this)) {
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightness
            )
        }
    }
}