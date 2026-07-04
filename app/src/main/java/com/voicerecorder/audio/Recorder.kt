package com.voicerecorder.audio

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import java.io.File

/**
 * Thin wrapper around [MediaRecorder] tuned for voice: mono AAC in an MP4 container at
 * 16 kHz / 64 kbps, which keeps files small while staying clear for speech. One instance
 * writes one segment file; the service creates a fresh Recorder per segment.
 */
class Recorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val isRecording: Boolean get() = recorder != null

    /** Begin writing to [file]. Throws if the mic can't be acquired. */
    fun start(file: File) {
        check(recorder == null) { "Recorder already started" }
        val mr = MediaRecorder(context).apply {
            // MIC is the general-purpose voice source. Switch to VOICE_RECOGNITION if you
            // want the OS to skip call-style processing (AGC/echo cancel) for a rawer take.
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioChannels(1)
            setAudioSamplingRate(16_000)
            setAudioEncodingBitRate(64_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mr
        outputFile = file
    }

    /**
     * Stop and finalize the current segment. Returns the written file, or null if the
     * take was too short to be valid (MediaRecorder throws on sub-second stops).
     */
    fun stop(): File? {
        val mr = recorder ?: return null
        val file = outputFile
        recorder = null
        outputFile = null
        return try {
            mr.stop()
            mr.release()
            file
        } catch (e: RuntimeException) {
            // stop() throws if start()..stop() captured no frames; discard the stub file.
            Log.w(TAG, "Discarding empty/short recording", e)
            mr.release()
            file?.delete()
            null
        }
    }

    private companion object {
        const val TAG = "Recorder"
    }
}
