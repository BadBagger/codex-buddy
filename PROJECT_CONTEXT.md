# Codex Buddy Project Context

## App

- Name: Codex Buddy
- Repo: `https://github.com/BadBagger/codex-buddy`
- Repo visibility: public, so DevHub can read GitHub release metadata without embedding a token in the Android app
- Android package: `com.softsmith.codexbuddy`
- Current version: `0.2.1-panel-history`
- Current role: Android local status bridge for existing Codex work notifications

## Current Release

- Tag: `v0.2.1-panel-history`
- APK assets:
  - `CodexBuddy.apk`
  - `CodexBuddy-release-v0.2.1-panel-history.apk`

## Product Boundary

This app is not the desktop Codex app or runtime. It is a native Android overlay and local HTTP listener for status notifications from the user's existing Codex work.

## Next Work

- Verify the packaged PC-side hook installer on-device against the phone listener URL.
- Replace debug signing with a private release keystore for production-style distribution.

## Recent Notes

- `v0.1.1-overlay-start-fix` adds the required Android foreground-service data sync permission so the overlay service can start cleanly on Android 14+.
- `v0.2.0-status-bridge` changes Codex Buddy from a separate OpenAI chat client into a local status listener for existing Codex work. It exposes `POST /notify` on port 8787 and includes a sample Codex `Stop` hook script.
- `tools/install-codex-buddy-hook.ps1` packages the PC-side setup: it copies the notifier into `~\.codex\tools\codex-buddy`, backs up `~\.codex\config.toml`, and points Codex `notify` at the installed wrapper.
- `v0.2.1-panel-history` persists recent status events so the floating panel shows the same updates as Android notifications after it is reopened.
