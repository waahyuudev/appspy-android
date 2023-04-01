package com.wahyu.trackapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat

class AutoStart : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        if (Intent.ACTION_BOOT_COMPLETED == intent?.action) {
            Log.d("AutoStart", "auto start called")
            val PERMISSIONS = arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_PHONE_STATE
            )
            if (hasPermissions(context, *PERMISSIONS)) {
                try {
                    val i = Intent(context, AppService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context?.startForegroundService(i)
                    } else {
                        context?.startService(i)
                    }
//                    context?.startService(i)
                    Log.i("Autostart", "started")
                } catch (e: Exception) {
                    e.printStackTrace();
                }
            }
        }
    }

    private fun hasPermissions(context: Context?, vararg permissions: String): Boolean =
        permissions.all {
            context?.let { it1 ->
                ActivityCompat.checkSelfPermission(
                    it1,
                    it
                )
            } == PackageManager.PERMISSION_GRANTED
        }
}