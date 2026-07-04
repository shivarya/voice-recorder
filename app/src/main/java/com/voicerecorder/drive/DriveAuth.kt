package com.voicerecorder.drive

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes

/**
 * Google sign-in + Drive client factory. We request only the narrow [DriveScopes.DRIVE_APPDATA]
 * scope, so the app can read/write nothing except its own hidden appDataFolder — recordings
 * never appear in the user's normal Drive and no other file is reachable.
 */
object DriveAuth {

    private val SCOPES = listOf(DriveScopes.DRIVE_APPDATA)
    private val DRIVE_SCOPE = Scope(DriveScopes.DRIVE_APPDATA)

    fun signInClient(context: Context): GoogleSignInClient {
        val opts = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(DRIVE_SCOPE)
            .build()
        return GoogleSignIn.getClient(context, opts)
    }

    fun lastAccount(context: Context): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    fun hasDriveScope(account: GoogleSignInAccount?): Boolean =
        account != null && GoogleSignIn.hasPermissions(account, DRIVE_SCOPE)

    /** Build an authenticated Drive client for [accountName], or null if none is set. */
    fun driveFor(context: Context, accountName: String?): Drive? {
        val name = accountName ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(context, SCOPES).apply {
            selectedAccountName = name
        }
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential,
        ).setApplicationName("Recorder").build()
    }
}
