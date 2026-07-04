package com.voicerecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.voicerecorder.core.Prefs
import com.voicerecorder.core.RecordingState
import com.voicerecorder.drive.DriveAuth
import com.voicerecorder.drive.UploadWorker
import com.voicerecorder.security.BiometricGate
import com.voicerecorder.service.RecordingService
import com.voicerecorder.storage.Recording
import com.voicerecorder.storage.RecordingStore
import com.voicerecorder.ui.HomeScreen
import com.voicerecorder.ui.LockedScreen
import com.voicerecorder.ui.theme.VoiceRecorderTheme

/**
 * The only window with UI. It's [FragmentActivity] because BiometricPrompt needs one, and
 * it sets FLAG_SECURE so recordings can't be screenshotted or previewed in Recents. All
 * content sits behind the biometric gate; recording itself starts from the tile/shortcut
 * without unlocking.
 */
class MainActivity : FragmentActivity() {

    private val prefs by lazy { Prefs(this) }

    private val unlocked = mutableStateOf(false)
    private val recordings = mutableStateOf<List<Recording>>(emptyList())
    private val driveEmail = mutableStateOf<String?>(null)
    private val playingName = mutableStateOf<String?>(null)

    private val resumeOnBoot = mutableStateOf(false)
    private val autoUpload = mutableStateOf(false)
    private val wifiOnly = mutableStateOf(true)
    private val deleteAfterUpload = mutableStateOf(false)

    private var player: MediaPlayer? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted[Manifest.permission.RECORD_AUDIO] == true) toggleRecord()
    }

    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        signInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result -> handleSignIn(result.data) }

        driveEmail.value = prefs.driveAccount ?: DriveAuth.lastAccount(this)?.email
        resumeOnBoot.value = prefs.resumeOnBoot
        autoUpload.value = prefs.autoUpload
        wifiOnly.value = prefs.wifiOnlyUpload
        deleteAfterUpload.value = prefs.deleteAfterUpload

        if (!BiometricGate.isAvailable(this)) unlocked.value = true // no lock set: can't gate

        enableEdgeToEdge()
        setContent {
            VoiceRecorderTheme {
                val isRecording by RecordingState.recording.collectAsStateWithLifecycle()
                val elapsed by RecordingState.elapsedMs.collectAsStateWithLifecycle()
                if (unlocked.value) {
                    HomeScreen(
                        recording = isRecording,
                        elapsedMs = elapsed,
                        recordings = recordings.value,
                        playingName = playingName.value,
                        driveEmail = driveEmail.value,
                        resumeOnBoot = resumeOnBoot.value,
                        autoUpload = autoUpload.value,
                        wifiOnly = wifiOnly.value,
                        deleteAfterUpload = deleteAfterUpload.value,
                        onToggleRecord = ::toggleRecord,
                        onPlay = ::togglePlay,
                        onDelete = ::delete,
                        onSignIn = ::startSignIn,
                        onSignOut = ::signOut,
                        onSyncNow = ::syncNow,
                        onSetResumeOnBoot = { prefs.resumeOnBoot = it; resumeOnBoot.value = it },
                        onSetAutoUpload = { prefs.autoUpload = it; autoUpload.value = it },
                        onSetWifiOnly = { prefs.wifiOnlyUpload = it; wifiOnly.value = it },
                        onSetDeleteAfterUpload = { prefs.deleteAfterUpload = it; deleteAfterUpload.value = it },
                    )
                } else {
                    LockedScreen(onUnlock = ::authenticate)
                }
            }
        }

        if (BiometricGate.isAvailable(this)) authenticate()
    }

    override fun onResume() {
        super.onResume()
        if (unlocked.value) refresh()
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    private fun authenticate() {
        BiometricGate.authenticate(
            activity = this,
            onSuccess = {
                unlocked.value = true
                refresh()
            },
            onError = { /* stays locked; user can retry via the Unlock button */ },
        )
    }

    private fun refresh() {
        recordings.value = RecordingStore.list(this)
    }

    // --- recording ---------------------------------------------------------

    private fun hasMic(): Boolean = ContextCompat.checkSelfPermission(
        this, Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    private fun neededPermissions(): Array<String> = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private fun toggleRecord() {
        if (!hasMic()) {
            permissionLauncher.launch(neededPermissions())
            return
        }
        if (RecordingState.recording.value) {
            RecordingService.stop(this)
        } else {
            RecordingService.start(this)
        }
    }

    // --- playback ----------------------------------------------------------

    private fun togglePlay(rec: Recording) {
        if (playingName.value == rec.name) {
            stopPlayback()
            return
        }
        stopPlayback()
        try {
            player = MediaPlayer().apply {
                setDataSource(rec.file.absolutePath)
                setOnCompletionListener { stopPlayback() }
                prepare()
                start()
            }
            playingName.value = rec.name
        } catch (e: Exception) {
            Toast.makeText(this, "Can't play this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopPlayback() {
        player?.release()
        player = null
        playingName.value = null
    }

    private fun delete(rec: Recording) {
        if (playingName.value == rec.name) stopPlayback()
        RecordingStore.delete(this, rec.name)
        refresh()
    }

    // --- Google Drive ------------------------------------------------------

    private fun startSignIn() {
        signInLauncher.launch(DriveAuth.signInClient(this).signInIntent)
    }

    private fun handleSignIn(data: Intent?) {
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            prefs.driveAccount = account.email
            driveEmail.value = account.email
            Toast.makeText(this, "Signed in as ${account.email}", Toast.LENGTH_SHORT).show()
        } catch (e: ApiException) {
            Toast.makeText(this, "Drive sign-in failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signOut() {
        DriveAuth.signInClient(this).signOut()
        prefs.driveAccount = null
        driveEmail.value = null
    }

    private fun syncNow() {
        if (driveEmail.value == null) {
            Toast.makeText(this, "Sign in to Drive first", Toast.LENGTH_SHORT).show()
            return
        }
        UploadWorker.enqueueAll(this)
        Toast.makeText(this, "Sync queued", Toast.LENGTH_SHORT).show()
    }
}
