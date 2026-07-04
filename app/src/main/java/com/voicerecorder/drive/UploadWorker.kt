package com.voicerecorder.drive

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.voicerecorder.core.Prefs
import com.voicerecorder.storage.RecordingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Uploads one recording segment to Drive, with retry/backoff and network constraints
 * managed by WorkManager so it survives process death and picks the right moment (e.g.
 * Wi-Fi only). Enqueued on segment rotation and from the "Sync now" action.
 */
class UploadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val name = inputData.getString(KEY_FILE_NAME) ?: return@withContext Result.failure()
        val ctx = applicationContext
        if (RecordingStore.isUploaded(ctx, name)) return@withContext Result.success()
        val file = RecordingStore.fileByName(ctx, name)
            ?: return@withContext Result.success() // deleted before upload; nothing to do

        val prefs = Prefs(ctx)
        val drive = DriveAuth.driveFor(ctx, prefs.driveAccount)
            ?: return@withContext Result.retry() // not signed in yet

        try {
            DriveUploader.upload(drive, file)
            RecordingStore.markUploaded(ctx, name)
            if (prefs.deleteAfterUpload) file.delete()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_FILE_NAME = "file_name"
        private const val TAG = "voice_upload"
        private const val MAX_ATTEMPTS = 5

        /** Queue an upload for a single recording (no-op-safe if already queued). */
        fun enqueue(context: Context, name: String) {
            val prefs = Prefs(context)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (prefs.wifiOnlyUpload) NetworkType.UNMETERED else NetworkType.CONNECTED,
                )
                .build()
            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_FILE_NAME to name))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(TAG)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("upload_$name", ExistingWorkPolicy.KEEP, request)
        }

        /** Queue uploads for every not-yet-uploaded recording ("Sync now"). */
        fun enqueueAll(context: Context) {
            RecordingStore.list(context)
                .filterNot { it.uploaded }
                .forEach { enqueue(context, it.name) }
        }
    }
}
