package com.example.flashlight

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var tvLoadingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_activity)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splashScreen)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
//        Handler(Looper.getMainLooper()).postDelayed({
//            startActivity(Intent(this, MainActivity::class.java))
//            finish()
//        }, 5000)
//        lifecycleScope.launch {
//            delay(2500)
//            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
//            finish()
//        }
        tvLoadingText = findViewById(R.id.tvLoadingText)

        // Dòng chữ hiển thị lần lượt
        val loadingSteps = listOf(
            "Xin chào",
            "Đang khởi tạo...",
            "Sắp xong...",
            "Xong rồi!"
        )

        lifecycleScope.launch {
            for ((i, text) in loadingSteps.withIndex()) {
                tvLoadingText.text = text
                delay(2000L) // Delay tăng dần
            }

//            delay(800)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }
}