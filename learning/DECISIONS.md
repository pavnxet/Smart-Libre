# LibreTube Architectural Decision Records (ADRs)

## ADR-001: Relocate Build Tools, JDK, and Gradle Cache to E: Drive
- **Context**: Drive C: has very limited free space (< 7 GB). Android SDK and Gradle caches can quickly exceed this.
- **Decision**: All build tools (JDK 17, Android SDK cmdline-tools, platform-tools) and `GRADLE_USER_HOME` have been permanently directed to `E:\`.
- **Status**: Implemented and verified.

## ADR-002: Dynamic OpenAI-Compatible Relay (ApiBeam) & SoundHelper Architecture
- **Context**: Users need to swap AI providers freely to ApiBeam time-based endpoints without rebuilding the APK, and audio playback must not fail or crash across diverse Android versions and OEM audio routers.
- **Decision**: 
  - Exposed `KEY_APIBEAM_URL` in `AiSettingsSheet` with instant user edits and dynamic endpoint formatting (`/chat/completions`).
  - Extracted audio playback into `SoundHelper.kt` with explicit `AudioAttributes` (`USAGE_MEDIA` / `CONTENT_TYPE_SONIFICATION`) and safe resource recycling.
- **Status**: Implemented in v1.0.3.

