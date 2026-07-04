package com.voicerecorder.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide source of truth shared between the UI, the Quick Settings tile, the
 * one-tap trampoline, and the foreground service. Kept as a singleton so the tile can
 * flip state and reflect it without the Activity being alive.
 */
object RecordingState {

    /** Whether a recording session is currently running. */
    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording.asStateFlow()

    /** Milliseconds elapsed in the current session (0 when stopped). */
    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    /** File name of the segment currently being written (null when stopped). */
    private val _currentFile = MutableStateFlow<String?>(null)
    val currentFile: StateFlow<String?> = _currentFile.asStateFlow()

    fun setRecording(value: Boolean) {
        _recording.value = value
        if (!value) {
            _elapsedMs.value = 0L
            _currentFile.value = null
        }
    }

    fun setElapsed(ms: Long) {
        _elapsedMs.value = ms
    }

    fun setCurrentFile(name: String?) {
        _currentFile.value = name
    }
}
