package com.voicerecorder.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.voicerecorder.QuickRecordActivity
import com.voicerecorder.R
import com.voicerecorder.core.Prefs

/**
 * Resumes recording after a reboot — but note: Android 12+ forbids starting a
 * microphone (while-in-use) foreground service from the background, and BOOT_COMPLETED
 * is a background start. So we can't legally auto-start the mic here. Instead, if the
 * user opted in, we post a single tap-to-resume notification; tapping it opens the
 * one-tap trampoline from a user gesture, which the OS *does* allow to start the service.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return
        if (!Prefs(context).resumeOnBoot) return

        createChannel(context)
        val tap = PendingIntent.getActivity(
            context,
            0,
            Intent(context, QuickRecordActivity::class.java)
                .setAction(QuickRecordActivity.ACTION_QUICK_RECORD)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = android.app.Notification.Builder(context, CHANNEL_ID)
            .setContentTitle("Resume recording?")
            .setContentText("Tap to start recording again after restart")
            .setSmallIcon(R.drawable.ic_record_tile)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIF_ID, notification)
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Resume after reboot", NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "Offers to resume recording after the device restarts" }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "resume_boot"
        const val NOTIF_ID = 8
    }
}
