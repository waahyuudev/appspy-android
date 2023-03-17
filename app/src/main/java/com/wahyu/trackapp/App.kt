package com.wahyu.trackapp

import android.app.Application
import android.util.Log

class App : Application() {
    override fun onCreate() {
        Log.d("AppStart", "Started")
        super.onCreate()
    }
}