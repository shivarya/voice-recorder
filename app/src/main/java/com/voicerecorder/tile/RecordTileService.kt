package com.voicerecorder.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.voicerecorder.QuickRecordActivity
import com.voicerecorder.core.RecordingState

/**
 * One-tap start/stop from the Quick Settings shade — the "one-click launch". Tapping
 * routes through [QuickRecordActivity] rather than starting the service directly, because
 * launching a microphone foreground service must originate from a user-visible context
 * (a tile tap alone isn't a guaranteed exemption on all OS versions); the trampoline is.
 */
class RecordTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        syncTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, QuickRecordActivity::class.java)
            .setAction(QuickRecordActivity.ACTION_QUICK_RECORD)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pi = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pi)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun syncTile() {
        val tile = qsTile ?: return
        val on = RecordingState.recording.value
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = if (on) "Recording" else "Tap to record"
        tile.updateTile()
    }
}
