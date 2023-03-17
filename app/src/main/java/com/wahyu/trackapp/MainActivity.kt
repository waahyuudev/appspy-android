package com.wahyu.trackapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startService(Intent(this, AppService::class.java))
        tryScheduler()

    }

    private fun  tryScheduler() {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                Log.d("MainActivity", "todo for hit background service repeat")
            }
        }, 2000)
    }
}