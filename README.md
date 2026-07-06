# Codex Buddy

Codex Buddy is a native Android overlay status companion for existing Codex work on a PC. It runs a local listener on the phone, shows a floating bubble over other apps, and lets a Codex desktop hook post completion updates to the phone.

## What works

- Launcher setup screen for overlay permission and service start/stop.
- Foreground overlay service with a persistent notification.
- Draggable floating bubble over other apps.
- Local HTTP listener on port `8787`.
- `GET /health` health check.
- `POST /notify` endpoint accepting JSON `title`, `message`, and `status`.
- Windows hook scripts for posting Codex completion notifications from the PC.

## Boundary

This is not the desktop Codex app/runtime on Android. It does not read or control other Android apps. Real app-reading or app-control behavior should be added later through explicit AccessibilityService flows, with clear user permission and safety boundaries.

## PC hook setup

1. Install and open `CodexBuddy.apk` on the phone.
2. Grant overlay permission, start the floating buddy, and note the listener URL shown in the app, for example `http://192.168.1.223:8787/notify`.
3. From this repo on the PC, install the Codex notification hook:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\install-codex-buddy-hook.ps1 -PhoneUrl "http://PHONE_IP:8787/notify"
```

The installer copies the notifier into `~\.codex\tools\codex-buddy`, backs up `~\.codex\config.toml`, and sets the Codex `notify` command to the installed wrapper.

To send a manual test notification:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "$env:USERPROFILE\.codex\tools\codex-buddy\codex-buddy-notify.ps1" -PhoneUrl "http://PHONE_IP:8787/notify" -Title "Codex Buddy test" -Message "PC hook installed." -Status "done"
```

## Build

```powershell
$env:JAVA_HOME='C:\Users\KyleB\Documents\Codex\2026-07-04\build-a-native-android-app-using\.local-jdk\jdk-17.0.19+10'
$env:ANDROID_HOME='C:\Users\KyleB\Documents\Codex\2026-07-04\build-a-native-android-app-using\.android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:assembleRelease
```
