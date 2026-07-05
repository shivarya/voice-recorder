---
name: voice-release
description: Build the signed release APK/AAB for Voice Recorder, verify the signature, report the signing SHA-1, and install/distribute. Use when shipping a voice-recorder build or when the release SHA-1 is needed for the Drive OAuth client.
---

Produce a production-signed Voice Recorder build. Package (applicationId):
`dev.shivarya.voicerecorder`; code namespace stays `com.voicerecorder`.

## Signing prerequisites

Release signing reads a **gitignored** `keystore.properties` at the project root:
```
storeFile=voice-recorder-release.jks
storePassword=<secret>
keyAlias=voicerecorder
keyPassword=<secret>
```
`app/build.gradle.kts` loads it into `signingConfigs.release` and signs the `release` build
type. Neither `voice-recorder-release.jks` nor `keystore.properties` is committed — **back
both up**; losing them means never being able to update the app under the same identity.

If the keystore is missing (fresh machine), regenerate the properties/keystore and re-run:
```powershell
$keytool = Join-Path $env:JAVA_HOME "bin\keytool.exe"
& $keytool -genkeypair -v -keystore voice-recorder-release.jks -alias voicerecorder `
    -keyalg RSA -keysize 2048 -validity 10000 -storepass <pw> -keypass <pw> `
    -dname "CN=Voice Recorder, O=Shivarya, C=IN"
```
Note: a **new** keystore produces a **new SHA-1**, which then needs a new Drive OAuth client.

## Build (signed release)

```powershell
cd "c:\Users\Ash\Documents\Projects\apps\voice-recorder" ; .\gradlew.bat :app:assembleRelease -x lint
```
- APK: `app\build\outputs\apk\release\app-release.apk` (~47 MB — R8/minify is off so the
  release build behaves identically to the smoke-tested debug build; enabling
  `isMinifyEnabled = true` will shrink it substantially but re-test Drive/reflection first).
- For Play Store upload use a bundle instead: `.\gradlew.bat :app:bundleRelease` →
  `app\build\outputs\bundle\release\app-release.aab`.

## Verify signature + read the SHA-1

```powershell
$bt = Get-ChildItem "D:\Android_SDK\build-tools" -Directory | Sort-Object Name -Descending | Select-Object -First 1
& (Join-Path $bt.FullName "apksigner.bat") verify --print-certs "app\build\outputs\apk\release\app-release.apk"
```
Current release signing fingerprints (keystore `voice-recorder-release.jks`, alias
`voicerecorder`) — **package name has no effect on these**:
- SHA-1: `EB:10:13:13:02:4E:AB:AE:09:3F:21:D1:CF:7D:DB:25:77:0B:ED:B1`
- SHA-256: `70:A9:5C:C5:19:F0:43:42:82:86:96:F8:4D:1B:57:34:1C:41:23:9D:46:20:04:21:E3:72:C9:5F:65:BD:66:C3`

## Install / distribute

```powershell
$adb = "D:\Android_SDK\platform-tools\adb.exe"
& $adb install -r "app\build\outputs\apk\release\app-release.apk"     # uninstall debug variant first if present
```
Sideload: copy `app-release.apk` to the phone and tap it. Play Store: upload the `.aab`.

## Google Drive OAuth (required for sync only)

The app authenticates the **Android OAuth client by package name + SHA-1** — there is **no
client ID or secret embedded in the app**. In Google Cloud Console:
1. Enable the **Drive API**.
2. OAuth consent screen: add scope `https://www.googleapis.com/auth/drive.appdata`; add your
   Google account under **Test users** while the app is in "Testing".
3. Create an **OAuth client ID → Android** with package `dev.shivarya.voicerecorder` and the
   release SHA-1 above. (A "Web application" client will NOT work — sign-in fails with
   `ApiException` status 10 / DEVELOPER_ERROR.)

If the package name ever changes, the SHA-1 stays the same but a new Android OAuth client
(new package + same SHA-1) must be registered.

## Rules

- Never commit the keystore or `keystore.properties` (both are gitignored — keep it that way).
- For real launcher/Play icons + feature graphic, use the root `play-store-assets` skill.
