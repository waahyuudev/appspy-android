package com.wahyu.trackapp

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            val p = packageManager
            val componentName = ComponentName(
                this,
                MainActivity::class.java
            ) // activity which is first time open in manifiest file which is declare as <category android:name="android.intent.category.LAUNCHER" />

            p.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
//        startService(Intent(this, AppService::class.java))

    }
}