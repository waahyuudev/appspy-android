package com.wahyu.trackapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class RestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("RestartReceiver", "Service restarted")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context?.startForegroundService(Intent(context, AppService::class.java))
        } else {
            context?.startService(Intent(context, AppService::class.java))
        }
    }
}