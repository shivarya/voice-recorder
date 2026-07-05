---
name: voice-dev
description: Build the Voice Recorder debug APK and install/launch it on a connected Android device or emulator. Use to develop or test voice-recorder locally.
---

Build and run the Voice Recorder (native Kotlin/Compose) app for local iteration.
Package (applicationId): `dev.shivarya.voicerecorder`; code namespace: `com.voicerecorder`.

## Build (debug)

```powershell
cd "c:\Users\Ash\Documents\Projects\apps\voice-recorder" ; .\gradlew.bat :app:assembleDebug -x lint
```
Debug APK lands at `app\build\outputs\apk\debug\app-debug.apk`. Native Kotlin — the
RN/CMake 260-char long-path junction trick is NOT needed here.

## Install + launch on a device

```powershell
$adb = "D:\Android_SDK\platform-tools\adb.exe"
& $adb devices -l                                          # confirm a device is attached
& $adb install -r "app\build\outputs\apk\debug\app-debug.apk"
& $adb shell am start -n dev.shivarya.voicerecorder/com.voicerecorder.MainActivity
```
The debug and release builds are signed with different keys, so if the other variant is
already installed, uninstall first: `& $adb uninstall dev.shivarya.voicerecorder`.

## Exercising the features

- **Record**: in-app button, the Quick Settings tile (add it from the QS edit tray), or
  long-press the launcher icon → "Start recording" shortcut. All start `RecordingService`
  (foreground, `microphone` type) — the ongoing notification + green mic dot are mandatory
  and cannot be removed.
- **Permissions**: first run prompts for `RECORD_AUDIO` + `POST_NOTIFICATIONS`. Grant via
  UI, or pre-grant: `& $adb shell pm grant dev.shivarya.voicerecorder android.permission.RECORD_AUDIO`.
- **Biometric gate**: the UI is behind `BiometricPrompt`; a screen lock (PIN/fingerprint)
  must be set or the app opens unlocked (nothing to authenticate against). `FLAG_SECURE`
  means screenshots/screen-record of the app are blank — expected.
- **Recordings** live in app-internal storage (`filesDir/recordings/*.m4a`), not readable
  by other apps or `adb pull` without run-as on a debug build:
  `& $adb shell run-as dev.shivarya.voicerecorder ls files/recordings`
- **Logs**: `& $adb logcat --pid=$(& $adb shell pidof -s dev.shivarya.voicerecorder)`

## Notes

- Drive sync needs the Google Cloud OAuth setup (see the `voice-release` skill and
  `CLAUDE.md`); everything else works without it.
- "Resume after reboot" can't auto-start the mic service (Android 12+ blocks background
  mic FGS starts) — it posts a tap-to-resume notification instead. Test by toggling it on,
  rebooting the device, and tapping the notification.
