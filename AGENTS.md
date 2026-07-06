# Codex Buddy Agent Instructions

Read `PROJECT_CONTEXT.md` before changing source, building release artifacts, or publishing GitHub Releases.

## Product Boundary

Codex Buddy is an Android overlay chat companion. It may sit over other apps and relay chat through the OpenAI API after the user grants overlay permission. Do not describe it as the desktop Codex runtime on Android.

Adding app-reading or app-control behavior requires explicit AccessibilityService work, clear user permission copy, and narrow user-approved actions.

## Build

Use the known local Android toolchain on this Windows machine:

```powershell
$env:JAVA_HOME='C:\Users\KyleB\Documents\Codex\2026-07-04\build-a-native-android-app-using\.local-jdk\jdk-17.0.19+10'
$env:ANDROID_HOME='C:\Users\KyleB\Documents\Codex\2026-07-04\build-a-native-android-app-using\.android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleRelease
```

Keep API keys, keystores, and local SDK paths out of commits.
