package com.voicerecorder.drive

import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import java.io.File
import com.google.api.services.drive.model.File as DriveFile

/** Uploads recording files into the app's private Drive appDataFolder. */
object DriveUploader {

    /** Upload [local] into appDataFolder and return the created Drive file id. */
    fun upload(drive: Drive, local: File): String {
        val metadata = DriveFile().apply {
            name = local.name
            parents = listOf("appDataFolder")
        }
        val content = FileContent("audio/mp4", local)
        val created = drive.files().create(metadata, content)
            .setFields("id")
            .execute()
        return created.id
    }
}
