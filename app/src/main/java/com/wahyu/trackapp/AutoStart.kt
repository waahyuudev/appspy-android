package com.wahyu.trackapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class AutoStart: BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        try{
            val intent = Intent(p0, AppService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                p0?.startForegroundService(intent)
            } else {
                p0?.startService(intent)
            }
            Log.i("Autostart", "started")
        }catch (e: Exception) {
            e.printStackTrace();
        }
    }
}