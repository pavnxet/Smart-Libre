# LibreTube Project Learnings

## System Configuration
- SDK Root: `E:\Android\Sdk`
- OpenJDK 17: `E:\Java\jdk-17.0.20.1+1`
- Gradle User Home: `E:\.gradle` (Redirected to protect drive C: from running out of space)

## Codebase Architecture
- Native Kotlin / Jetpack Compose Android app.
- Multi-module gradle configuration (`app`, `baselineprofile`).
- ApiBeam Relay: OpenAI compatible `/chat/completions` endpoint running over websocket room relay; requires 0 auth or dummy token.
- Android Raw Audio Playback: Use explicit `AudioAttributes` (`USAGE_MEDIA`, `CONTENT_TYPE_SONIFICATION`) and `openRawResourceFd` rather than raw resource ID references to avoid OEM audio focus/stream muting.

