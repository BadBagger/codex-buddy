# Codex Buddy

Codex Buddy is a native Android overlay chat prototype. It adds a draggable floating bubble that can sit over other apps and relay messages to the OpenAI Responses API using an API key stored in private app preferences.

## What works

- Launcher setup screen for API key, model, overlay permission, and service start/stop.
- Foreground overlay service with a persistent notification.
- Draggable bubble over other apps.
- Expandable chat panel with message history for the current service session.
- Direct OpenAI API relay through `https://api.openai.com/v1/responses`.

## Boundary

This is not the desktop Codex app/runtime on Android. It is an Android companion overlay that can chat through the OpenAI API. Real app-reading or app-control behavior should be added later through explicit AccessibilityService flows, with clear user permission and safety boundaries.

## Build

```powershell
$env:JAVA_HOME='C:\Users\KyleB\Documents\Codex\2026-07-04\build-a-native-android-app-using\.local-jdk\jdk-17.0.19+10'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:assembleRelease
```
