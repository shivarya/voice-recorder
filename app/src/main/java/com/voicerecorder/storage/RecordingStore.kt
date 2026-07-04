package com.voicerecorder.storage

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A recording file plus its metadata and Drive-upload state. */
data class Recording(
    val name: String,
    val file: File,
    val sizeBytes: Long,
    val lastModified: Long,
    val uploaded: Boolean,
)

/**
 * Owns the on-disk layout for recordings. Files live in the app's private internal
 * storage (`filesDir/recordings/`), so no other app can read them; a biometric gate on
 * the UI is the only way in. Upload state is tracked in a small SharedPreferences set.
 */
object RecordingStore {

    private const val DIR = "recordings"
    private const val EXT = ".m4a"
    private const val UPLOAD_PREFS = "voice_recorder_uploads"
    private const val KEY_UPLOADED = "uploaded_names"

    fun dir(context: Context): File =
        File(context.filesDir, DIR).apply { mkdirs() }

    /** Allocate a fresh, timestamped segment file (not yet created on disk). */
    fun newSegmentFile(context: Context): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir(context), "rec_$stamp$EXT")
    }

    /** All recordings, newest first. */
    fun list(context: Context): List<Recording> {
        val uploaded = uploadedNames(context)
        return dir(context).listFiles { f -> f.isFile && f.name.endsWith(EXT) }
            ?.sortedByDescending { it.lastModified() }
            ?.map {
                Recording(
                    name = it.name,
                    file = it,
                    sizeBytes = it.length(),
                    lastModified = it.lastModified(),
                    uploaded = it.name in uploaded,
                )
            }
            ?: emptyList()
    }

    fun fileByName(context: Context, name: String): File? =
        File(dir(context), name).takeIf { it.exists() && it.name.endsWith(EXT) }

    fun delete(context: Context, name: String): Boolean {
        clearUploaded(context, name)
        return File(dir(context), name).delete()
    }

    // --- upload bookkeeping -------------------------------------------------

    fun isUploaded(context: Context, name: String): Boolean =
        name in uploadedNames(context)

    fun markUploaded(context: Context, name: String) {
        val sp = context.getSharedPreferences(UPLOAD_PREFS, Context.MODE_PRIVATE)
        val set = sp.getStringSet(KEY_UPLOADED, emptySet())!!.toMutableSet()
        set.add(name)
        sp.edit().putStringSet(KEY_UPLOADED, set).apply()
    }

    private fun clearUploaded(context: Context, name: String) {
        val sp = context.getSharedPreferences(UPLOAD_PREFS, Context.MODE_PRIVATE)
        val set = sp.getStringSet(KEY_UPLOADED, emptySet())!!.toMutableSet()
        if (set.remove(name)) sp.edit().putStringSet(KEY_UPLOADED, set).apply()
    }

    private fun uploadedNames(context: Context): Set<String> =
        context.getSharedPreferences(UPLOAD_PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_UPLOADED, emptySet()) ?: emptySet()
}
