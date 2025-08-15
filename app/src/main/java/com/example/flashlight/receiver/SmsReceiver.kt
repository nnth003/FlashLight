package com.example.flashlight.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.flashlight.service.FlashService

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            Log.d("SmsReceiver", "SMS received")
            val serviceIntent = Intent(context, FlashService::class.java).apply {
                action = "SMS_RECEIVED"
            }
            context.startService(serviceIntent)
        }
    }
}