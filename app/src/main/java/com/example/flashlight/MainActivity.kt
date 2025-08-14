package com.example.flashlight

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.flashlight.fragment.FlashFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bottomNavigation()
    }

    fun bottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
        val navHostController =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment
        navController = navHostController.navController

        bottomNavigation.setupWithNavController(navController)

//        // Load fragment mặc định
//        supportFragmentManager.beginTransaction()
//            .replace(R.id.frame_layout, FlashFragment())
//            .commit()
//
//        bottomNavigation.setOnItemSelectedListener { item ->
//            val selectedFragment = when (item.itemId) {
//                R.id.bottom_home -> FlashFragment()
//                R.id.bottom_search -> FlashFragment()
//                R.id.bottom_settings -> FlashFragment()
//                R.id.bottom_profile -> FlashFragment()
//                else -> null
//            }
//            selectedFragment?.let {
//                supportFragmentManager.beginTransaction()
//                    .replace(R.id.frame_layout, it)
//                    .commit()
//            }
//            true
//        }

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                0
            )
            // Trả lại insets không bị tiêu thụ
            insets
        }
    }
}