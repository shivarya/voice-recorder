package com.voicerecorder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.voicerecorder.storage.Recording
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LockedScreen(onUnlock: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.Lock, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(16.dp))
            Text("Locked", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.size(8.dp))
            Text(
                "Recordings are protected. Verify to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(24.dp))
            Button(onClick = onUnlock) { Text("Unlock") }
        }
    }
}

@Composable
fun HomeScreen(
    recording: Boolean,
    elapsedMs: Long,
    recordings: List<Recording>,
    playingName: String?,
    driveEmail: String?,
    resumeOnBoot: Boolean,
    autoStartOnOpen: Boolean,
    autoUpload: Boolean,
    wifiOnly: Boolean,
    deleteAfterUpload: Boolean,
    onToggleRecord: () -> Unit,
    onPlay: (Recording) -> Unit,
    onDelete: (Recording) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: () -> Unit,
    onSetResumeOnBoot: (Boolean) -> Unit,
    onSetAutoStartOnOpen: (Boolean) -> Unit,
    onSetAutoUpload: (Boolean) -> Unit,
    onSetWifiOnly: (Boolean) -> Unit,
    onSetDeleteAfterUpload: (Boolean) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Mic, null) },
                    label = { Text("Record") },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                    label = { Text("Recordings") },
                )
            }
        },
    ) { innerPadding ->
        Surface(
            Modifier.fillMaxSize().padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            if (selectedTab == 0) {
                RecordTab(
                    recording = recording,
                    elapsedMs = elapsedMs,
                    driveEmail = driveEmail,
                    resumeOnBoot = resumeOnBoot,
                    autoStartOnOpen = autoStartOnOpen,
                    autoUpload = autoUpload,
                    wifiOnly = wifiOnly,
                    deleteAfterUpload = deleteAfterUpload,
                    onToggleRecord = onToggleRecord,
                    onSignIn = onSignIn,
                    onSignOut = onSignOut,
                    onSyncNow = onSyncNow,
                    onSetResumeOnBoot = onSetResumeOnBoot,
                    onSetAutoStartOnOpen = onSetAutoStartOnOpen,
                    onSetAutoUpload = onSetAutoUpload,
                    onSetWifiOnly = onSetWifiOnly,
                    onSetDeleteAfterUpload = onSetDeleteAfterUpload,
                )
            } else {
                RecordingsTab(
                    recordings = recordings,
                    playingName = playingName,
                    onPlay = onPlay,
                    onDelete = onDelete,
                )
            }
        }
    }
}

@Composable
private fun RecordTab(
    recording: Boolean,
    elapsedMs: Long,
    driveEmail: String?,
    resumeOnBoot: Boolean,
    autoStartOnOpen: Boolean,
    autoUpload: Boolean,
    wifiOnly: Boolean,
    deleteAfterUpload: Boolean,
    onToggleRecord: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: () -> Unit,
    onSetResumeOnBoot: (Boolean) -> Unit,
    onSetAutoStartOnOpen: (Boolean) -> Unit,
    onSetAutoUpload: (Boolean) -> Unit,
    onSetWifiOnly: (Boolean) -> Unit,
    onSetDeleteAfterUpload: (Boolean) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Clear Audio", style = MaterialTheme.typography.headlineMedium)
        }
        item { RecordCard(recording, elapsedMs, onToggleRecord) }
        item {
            DriveCard(driveEmail, onSignIn, onSignOut, onSyncNow)
        }
        item {
            SettingsCard(
                resumeOnBoot, autoStartOnOpen, autoUpload, wifiOnly, deleteAfterUpload,
                onSetResumeOnBoot, onSetAutoStartOnOpen, onSetAutoUpload, onSetWifiOnly, onSetDeleteAfterUpload,
            )
        }
        item {
            Text(
                "For recording your own conversations and notes. Recording others " +
                    "without consent may be illegal where you live.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun RecordingsTab(
    recordings: List<Recording>,
    playingName: String?,
    onPlay: (Recording) -> Unit,
    onDelete: (Recording) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Recordings", style = MaterialTheme.typography.titleMedium)
        }
        if (recordings.isEmpty()) {
            item {
                Text(
                    "No recordings yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(recordings, key = { it.name }) { rec ->
            RecordingRow(
                rec = rec,
                playing = rec.name == playingName,
                onPlay = { onPlay(rec) },
                onDelete = { onDelete(rec) },
            )
        }
    }
}

@Composable
private fun RecordCard(recording: Boolean, elapsedMs: Long, onToggle: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (recording) formatElapsed(elapsedMs) else "Ready",
                style = MaterialTheme.typography.displaySmall,
                fontFamily = FontFamily.Monospace,
            )
            Button(onClick = onToggle) {
                Icon(if (recording) Icons.Filled.Stop else Icons.Filled.Mic, null)
                Spacer(Modifier.width(8.dp))
                Text(if (recording) "Stop" else "Record")
            }
            Text(
                if (recording) "Recording — keeps going if you close the app" else
                    "Also startable from the Quick Settings tile or home-screen shortcut",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DriveCard(
    email: String?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Google Drive backup", style = MaterialTheme.typography.titleMedium)
            Text(
                email?.let { "Signed in: $it" } ?: "Not signed in",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (email == null) {
                    Button(onClick = onSignIn) { Text("Sign in") }
                } else {
                    Button(onClick = onSyncNow) {
                        Icon(Icons.Filled.CloudUpload, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sync now")
                    }
                    OutlinedButton(onClick = onSignOut) { Text("Sign out") }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    resumeOnBoot: Boolean,
    autoStartOnOpen: Boolean,
    autoUpload: Boolean,
    wifiOnly: Boolean,
    deleteAfterUpload: Boolean,
    onSetResumeOnBoot: (Boolean) -> Unit,
    onSetAutoStartOnOpen: (Boolean) -> Unit,
    onSetAutoUpload: (Boolean) -> Unit,
    onSetWifiOnly: (Boolean) -> Unit,
    onSetDeleteAfterUpload: (Boolean) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Settings", style = MaterialTheme.typography.titleMedium)
            ToggleRow("Auto-start recording when app opens", autoStartOnOpen, onSetAutoStartOnOpen)
            ToggleRow("Auto-upload each segment", autoUpload, onSetAutoUpload)
            ToggleRow("Upload on Wi-Fi only", wifiOnly, onSetWifiOnly)
            ToggleRow("Delete local file after upload", deleteAfterUpload, onSetDeleteAfterUpload)
            ToggleRow("Offer to resume after reboot", resumeOnBoot, onSetResumeOnBoot)
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun RecordingRow(
    rec: Recording,
    playing: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPlay) {
                Icon(if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, "Play")
            }
            Column(Modifier.weight(1f)) {
                Text(rec.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(
                    "${formatDate(rec.lastModified)} · ${formatSize(rec.sizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            UploadBadge(rec.uploaded)
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Delete")
            }
        }
    }
}

@Composable
private fun UploadBadge(uploaded: Boolean) {
    val icon: ImageVector = if (uploaded) Icons.Filled.CloudDone else Icons.Filled.CloudUpload
    val tint = if (uploaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(Modifier.padding(horizontal = 4.dp)) {
        Icon(icon, if (uploaded) "Uploaded" else "Not uploaded", tint = tint)
    }
}

private fun formatElapsed(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}

private fun formatDate(epochMs: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(epochMs))
