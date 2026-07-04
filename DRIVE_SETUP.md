# Google Drive setup (step by step)

The app backs up recordings to a **private appDataFolder** in your Google Drive.
It uses `GoogleSignIn` + the Drive REST API (no Firebase, no `google-services.json`,
no client secret shipped in the app). An Android OAuth client is validated purely by
your app's **package name** + **signing certificate SHA-1**. You must create that
OAuth client once. ~10 minutes.

Package name: `com.voicerecorder`
Scope used: `https://www.googleapis.com/auth/drive.appdata` (app-private folder only)

---

## 1. Get your signing SHA-1

You need the SHA-1 of the keystore that signs the build you'll run.

**Debug build** (for testing on your own device/emulator):
```powershell
keytool -list -v `
  -keystore "$env:USERPROFILE\.android\debug.keystore" `
  -alias androiddebugkey -storepass android -keypass android
```

**Release build** (for Play Store): use your release keystore instead:
```powershell
keytool -list -v -keystore "path\to\release.jks" -alias <your-alias>
```

If you distribute through **Google Play App Signing**, also add the SHA-1 that Play
shows under *Play Console → your app → Test and release → App integrity → App signing
key certificate*. (Play re-signs your app, so the on-device SHA-1 is Play's, not yours.)

Copy the `SHA1:` line (e.g. `AB:CD:...:EF`).

---

## 2. Create / choose a Google Cloud project

1. Go to <https://console.cloud.google.com/>.
2. Top bar → project picker → **New Project** (or reuse an existing one). Name it
   e.g. `voice-recorder`. Create, then select it.

---

## 3. Enable the Drive API

1. Left menu → **APIs & Services → Library**.
2. Search **Google Drive API** → open it → **Enable**.

---

## 4. Configure the OAuth consent screen

1. **APIs & Services → OAuth consent screen**.
2. User type: **External** → Create.
3. Fill the required fields (app name `Recorder`, your support email, developer email).
4. **Scopes** → *Add or remove scopes* → add:
   `https://www.googleapis.com/auth/drive.appdata`
   (it's a **non-sensitive/limited** scope — it grants access only to the app's own
   hidden folder, so it does **not** require Google's app-verification review.)
5. **Test users**: while the app is in "Testing" mode, add every Google account you'll
   sign in with (including your own). Save.
   - You can stay in Testing indefinitely for personal use. Only "Publishing" to
     production would matter for other users, and `drive.appdata` avoids the heavy
     verification even then.

---

## 5. Create the Android OAuth client ID

1. **APIs & Services → Credentials → Create credentials → OAuth client ID**.
2. Application type: **Android**.
3. **Package name**: `com.voicerecorder`
4. **SHA-1 certificate fingerprint**: paste the SHA-1 from step 1.
5. Create. (No client secret is generated or needed for Android clients.)

That's it — the app now recognizes your device/build. Nothing to paste back into the
code; the OAuth client is matched by package name + SHA-1 at sign-in time.

---

## 6. Use it in the app

1. Build/install the app (see `CLAUDE.md`).
2. Open the app (unlock with biometrics) → **Google Drive backup → Sign in**.
3. Pick the account you added as a test user, and approve the appdata permission.
4. Toggle **Auto-upload each segment** (uploads new segments automatically) and/or tap
   **Sync now** (uploads everything not yet uploaded).
5. Optional: **Upload on Wi-Fi only** and **Delete local file after upload**.

---

## 7. Verify the uploads

Recordings go to the hidden appDataFolder, so they **do not** appear at
drive.google.com. To confirm they're there:

- In-app: each recording shows a cloud-check badge once uploaded.
- Via API (OAuth Playground or a quick script), list the space:
  ```
  GET https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&fields=files(id,name,size)
  ```
  Authorize with the `drive.appdata` scope for the same account.

To wipe backups, uninstalling the app removes its appDataFolder automatically, or you
can revoke access at <https://myaccount.google.com/permissions>.

---

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| Sign-in dialog closes with "Drive sign-in failed" | OAuth client SHA-1/package mismatch (wrong keystore), or the account isn't a **test user**. Re-check steps 4–5. `ApiException` status `10` = DEVELOPER_ERROR = SHA-1/package wrong. |
| Sign-in works but uploads never appear | Drive API not enabled (step 3), or the account has no network under the chosen constraint (Wi-Fi-only toggle). |
| "Access blocked: app not verified" | You're signing in with an account not on the test-users list while in Testing. Add it (step 4). |
| Uploads retry forever | The worker retries until an account is set; sign in to Drive, then tap **Sync now**. |
