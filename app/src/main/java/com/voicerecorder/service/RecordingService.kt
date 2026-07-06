package com.voicerecorder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.voicerecorder.MainActivity
import com.voicerecorder.R
import com.voicerecorder.audio.Recorder
import com.voicerecorder.core.Prefs
import com.voicerecorder.core.RecordingState
import com.voicerecorder.drive.UploadWorker
import com.voicerecorder.storage.RecordingStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Foreground, microphone-typed service that owns the actual recording. It keeps running
 * while the app is closed, the screen is off, and through Doze, until the user stops it.
 * Recording is split into fixed-length segments so a crash loses at most one segment and
 * uploads can happen incrementally. The persistent notification is mandatory for a
 * microphone foreground service (Android 9+) and doubles as the honest "mic is live" signal.
 */
class RecordingService : LifecycleService() {

    private lateinit var prefs: Prefs
    private var recorder: Recorder? = null
    private var currentFile: File? = null
    private var loopJob: Job? = null
    private var sessionStart = 0L
    private var segmentStart = 0L

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopRecording()
            return START_NOT_STICKY
        }
        if (recorder == null) startRecording()
        return START_STICKY
    }

    private fun startRecording() {
        startForegroundCompat()
        sessionStart = SystemClock.elapsedRealtime()
        if (!beginSegment()) return // beginSegment already tore us down on failure
        RecordingState.setRecording(true)
        loopJob = lifecycleScope.launch {
            while (isActive) {
                delay(1_000)
                val now = SystemClock.elapsedRealtime()
                RecordingState.setElapsed(now - sessionStart)
                if (now - segmentStart >= SEGMENT_MS) rotateSegment()
                updateNotification()
            }
        }
    }

    /** Start a new segment file. Returns false (and stops the service) if the mic fails. */
    private fun beginSegment(): Boolean {
        val file = RecordingStore.newSegmentFile(this)
        return try {
            recorder = Recorder(this).also { it.start(file) }
            currentFile = file
            segmentStart = SystemClock.elapsedRealtime()
            RecordingState.setCurrentFile(file.name)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Could not start recording", e)
            stopRecording()
            false
        }
    }

    private fun rotateSegment() {
        finishSegment()
        beginSegment()
    }

    /** Finalize the current segment and queue its upload if auto-upload is on. */
    private fun finishSegment() {
        val done = recorder?.stop()
        recorder = null
        currentFile = null
        if (done != null && prefs.autoUpload) {
            UploadWorker.enqueue(this, done.name)
        }
    }

    private fun stopRecording() {
        loopJob?.cancel()
        loopJob = null
        finishSegment()
        RecordingState.setRecording(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        loopJob?.cancel()
        loopJob = null
        recorder?.stop()
        recorder = null
        RecordingState.setRecording(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val elapsed = RecordingState.elapsedMs.value
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, RecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Clear Audio")
            .setContentText(formatElapsed(elapsed))
            .setSmallIcon(R.drawable.ic_record_tile)
            .setOngoing(true)
            .setUsesChronometer(false)
            .setContentIntent(openIntent)
            .addAction(Notification.Action.Builder(null, "Stop", stopIntent).build())
            .build()
    }

    private fun createChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID, "Clear Audio", NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "Shows while a recording is in progress" }
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_STOP = "com.voicerecorder.STOP"
        private const val TAG = "RecordingService"
        private const val CHANNEL_ID = "recording"
        private const val NOTIF_ID = 7
        private val SEGMENT_MS = TimeUnit.MINUTES.toMillis(15)

        fun start(context: Context) {
            context.startForegroundService(Intent(context, RecordingService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, RecordingService::class.java).setAction(ACTION_STOP),
            )
        }

        private fun formatElapsed(ms: Long): String {
            val totalSec = ms / 1000
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
        }
    }
}
