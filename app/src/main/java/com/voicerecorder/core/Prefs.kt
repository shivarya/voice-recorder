package com.voicerecorder.core

import android.content.Context

/** Tiny SharedPreferences wrapper for the app's settings. */
class Prefs(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("voice_recorder", Context.MODE_PRIVATE)

    /** Resume recording automatically after a device reboot. */
    var resumeOnBoot: Boolean
        get() = sp.getBoolean(KEY_RESUME_ON_BOOT, false)
        set(value) = sp.edit().putBoolean(KEY_RESUME_ON_BOOT, value).apply()

    /** Auto-upload each finished segment to Google Drive. */
    var autoUpload: Boolean
        get() = sp.getBoolean(KEY_AUTO_UPLOAD, false)
        set(value) = sp.edit().putBoolean(KEY_AUTO_UPLOAD, value).apply()

    /** Only upload over un-metered (Wi-Fi) connections. */
    var wifiOnlyUpload: Boolean
        get() = sp.getBoolean(KEY_WIFI_ONLY, true)
        set(value) = sp.edit().putBoolean(KEY_WIFI_ONLY, value).apply()

    /** Delete the local file once it has uploaded successfully. */
    var deleteAfterUpload: Boolean
        get() = sp.getBoolean(KEY_DELETE_AFTER_UPLOAD, false)
        set(value) = sp.edit().putBoolean(KEY_DELETE_AFTER_UPLOAD, value).apply()

    /** Email of the signed-in Google account used for Drive, or null. */
    var driveAccount: String?
        get() = sp.getString(KEY_DRIVE_ACCOUNT, null)
        set(value) = sp.edit().putString(KEY_DRIVE_ACCOUNT, value).apply()

    companion object {
        private const val KEY_RESUME_ON_BOOT = "resume_on_boot"
        private const val KEY_AUTO_UPLOAD = "auto_upload"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_DELETE_AFTER_UPLOAD = "delete_after_upload"
        private const val KEY_DRIVE_ACCOUNT = "drive_account"
    }
}
