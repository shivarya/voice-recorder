# CLAUDE.md — Voice Recorder

A private, secure personal voice recorder for Android. Native Kotlin + Jetpack
Compose (Material3), same toolchain as `clear-mic-router/` (AGP 8.7.3, Kotlin
2.0.21, Gradle 8.14.3, compileSdk 35, minSdk 31, targetSdk 35, JDK 17).

## What it does
- **Background recording** — a microphone-typed foreground service keeps recording
  while the app is closed, the screen is off, and through Doze. Recording is split
  into 15-minute segments (bounds crash loss; enables incremental upload).
- **Voice-optimized** — mono AAC in MP4 (`.m4a`) at 16 kHz / 64 kbps.
- **One-tap start/stop** — Quick Settings tile + home-screen shortcut, both routed
  through the invisible `QuickRecordActivity` trampoline (so the OS always permits
  the foreground-service start).
- **Biometric gate** — the UI (playback/list/settings/Drive) is behind
  `BiometricPrompt` (strong biometric or device credential). Starting a recording is
  NOT gated (fast capture); only *viewing* recordings is.
- **Google Drive backup** — uploads to the private `appDataFolder` via WorkManager,
  with Wi-Fi-only / auto-upload / delete-after-upload options.

## Scope boundary (do not cross)
The original request included "stay hidden while always recording." That is the
covert-surveillance / stalkerware pattern and is intentionally **not** built:
- no launcher-icon hiding,
- no suppression of the Android mic indicator / Privacy Dashboard,
- no recording without the mandatory foreground-service notification.
Modern Android enforces all of the above anyway, and Google Play bans icon-hiding.
"Hidden" here means **privacy-protected**: biometric lock, `FLAG_SECURE` (no
screenshots / blank Recents thumbnail), private Drive folder, neutral name/icon,
minimal notification. It is for recording **your own** conversations/notes.

## Architecture (`app/src/main/java/com/voicerecorder/`)
- `MainActivity` — `FragmentActivity` (BiometricPrompt requires one); sets
  `FLAG_SECURE`; hosts the Compose UI; owns playback (`MediaPlayer`), permission
  requests, and Google sign-in.
- `QuickRecordActivity` — NoDisplay trampoline; toggles recording; never gated.
- `service/RecordingService` — `LifecycleService`, `foregroundServiceType=microphone`,
  `START_STICKY`; segment rotation + elapsed timer in a `lifecycleScope` loop;
  ongoing notification with a Stop action.
- `service/BootReceiver` — on `BOOT_COMPLETED`, if `resumeOnBoot` is on, posts a
  tap-to-resume notification. (Android 12+ forbids starting a while-in-use mic FGS
  from the background, so auto-start at boot is impossible; the tap is the workaround.)
- `tile/RecordTileService` — Quick Settings one-tap toggle.
- `audio/Recorder` — `MediaRecorder` wrapper.
- `core/RecordingState` — process-wide `StateFlow` singleton (recording/elapsed/file).
- `core/Prefs` — `SharedPreferences` wrapper (settings incl. `autoStartOnOpen` +
  Drive account). `autoStartOnOpen` (opt-in, default off) makes `MainActivity` begin
  recording on open/unlock via `maybeAutoStart()` — the literal "always record" mode.
- `storage/RecordingStore` — files in `filesDir/recordings/`; upload-state tracking.
- `security/BiometricGate` — `BiometricPrompt` wrapper.
- `drive/DriveAuth` — Google sign-in (scope `DRIVE_APPDATA`) + Drive client factory.
- `drive/DriveUploader` — Drive v3 create into `appDataFolder`.
- `drive/UploadWorker` — WorkManager `CoroutineWorker` (constraints, retry, dedupe).
- `ui/HomeScreen` + `ui/theme/Theme` — Compose Material3 (dynamic color).

## Build / run (Windows)
```powershell
cd "c:\Users\Ash\Documents\Projects\apps\voice-recorder" ; .\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```
Native Kotlin, so the RN/CMake 260-char long-path junction trick is NOT needed here.

## Google Drive setup (required before sync works)
Drive uploads need a Google Cloud OAuth client tied to this app's signing cert.
`google-services.json` is NOT required (we use `GoogleSignIn` + Drive REST, not
Firebase), but the OAuth client must exist:
1. In Google Cloud Console, create/choose a project and **enable the Drive API**.
2. Configure the **OAuth consent screen**; add the scope
   `https://www.googleapis.com/auth/drive.appdata`. While in "testing", add your
   Google account as a test user.
3. Create an **OAuth client ID → Android**, with:
   - package name `com.voicerecorder`
   - the **SHA-1** of the signing key. Debug:
     `keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android`
     (use the release keystore's SHA-1 for release builds).
No client secret is embedded in the app — the Android OAuth client is validated by
package name + SHA-1. Recordings land in the hidden appDataFolder; verify via the
Drive API with `files.list(spaces="appDataFolder")` (they won't show in drive.google.com).

## Known platform constraints
- **No true auto-record on boot**: mic foreground services can't be started from the
  background on Android 12+. `BootReceiver` posts a tap-to-resume notification instead.
- **Mic indicator + notification are mandatory** and cannot be removed — by design.
- **Re-lock policy**: the app unlocks once per activity instance; it does not re-lock
  on every return to foreground (avoids a device-credential re-prompt loop). Tighten
  later if desired.

## Next steps / not yet done
- Real app icon + Play assets (via the root `play-store-assets` skill / a
  `scripts/generate-icons.js`).
- Optional: resumable/chunked Drive upload for very large segments; a foreground
  "uploading" indicator; per-recording rename/notes.
