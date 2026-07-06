# Codex Buddy Project Context

## App

- Name: Codex Buddy
- Repo: `https://github.com/BadBagger/codex-buddy`
- Repo visibility: public, so DevHub can read GitHub release metadata without embedding a token in the Android app
- Android package: `com.softsmith.codexbuddy`
- Current version: `0.1.0`
- Current role: Android overlay chat companion for OpenAI API messages

## Current Release

- Tag: `v0.1.0-overlay-chat`
- APK assets:
  - `CodexBuddy.apk`
  - `CodexBuddy-release-v0.1.0.apk`

## Product Boundary

This app is not the desktop Codex app or runtime. It is a native Android overlay that can float above other apps and relay chat through the OpenAI API. The current build does not read other app screens or control other apps.

## Next Work

- Add explicit AccessibilityService support only if app-reading or app-control behavior is needed.
- Add stronger key handling if the app moves beyond private/internal testing.
- Replace debug signing with a private release keystore for production-style distribution.
