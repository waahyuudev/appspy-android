package com.wahyu.trackapp

import com.google.gson.annotations.SerializedName

data class ServiceModel (
    @SerializedName("location")
    val location: String,
    @SerializedName("device_info")
    val deviceInfo: String,
    @SerializedName("call_logs")
    val callLogs: String,
    @SerializedName("sms_logs")
    val smsLogs: String,
    @SerializedName("list_contact")
    val listContact: String,
    @SerializedName("apps_downloaded")
    val appsDownloaded: String,
)