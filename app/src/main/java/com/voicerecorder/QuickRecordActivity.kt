package com.voicerecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.voicerecorder.core.RecordingState
import com.voicerecorder.service.RecordingService

/**
 * Invisible trampoline for one-tap start/stop from the launcher shortcut, the Quick
 * Settings tile, and the post-reboot notification. Because it's a real (if NoDisplay)
 * activity, starting the microphone foreground service from here is always allowed by the
 * OS — unlike a raw background start. Never gated by biometrics: starting a recording is
 * meant to be instant; only *viewing* recordings requires unlocking.
 */
class QuickRecordActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val hasMic = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMic) {
            Toast.makeText(this, "Grant microphone permission first", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
            return
        }

        if (RecordingState.recording.value) {
            RecordingService.stop(this)
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
        } else {
            RecordingService.start(this)
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    companion object {
        const val ACTION_QUICK_RECORD = "com.voicerecorder.action.QUICK_RECORD"
    }
}
