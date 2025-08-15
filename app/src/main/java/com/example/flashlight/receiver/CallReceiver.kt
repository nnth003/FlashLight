package com.example.flashlight.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("flashlight_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("flash_on_call", false)) return

        // Gọi logic nháy đèn nếu có cuộc gọi đến
        if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
//                FlashHelper.blinkFlash(context)
            }
        }
    }
}