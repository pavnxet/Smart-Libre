# LibreTube Session History

## Session 2026-09-11
- Cloned LibreTube from official repository into `E:\Codes\Testing\LibreTube`.
- Initialized isolated project memory files and `AGENTS.md`.
- Verified OpenJDK 17 and Android SDK platform-tools configuration on `E:\`.
## Session 2026-09-11 (Update)
- Ran graphify AST extraction on LibreTube codebase (458 source files).
- Generated interactive knowledge graph at `graphify-out/graph.html` (3,538 nodes, 7,825 edges, 219 communities).
## Session 2026-09-11 (Feature Implementation & Build Complete)
- Implemented "Live In-Player Timestamps Sidebar" feature in LibreTube:
  - Created `TimestampItem.kt` model with timestamp regex parser (`mm:ss`, `hh:mm:ss`, notes).
  - Designed `view_timestamps_sidebar.xml` right-docked translucent floating overlay and `item_timestamp_entry.xml`.
  - Built `TimestampsSidebarAdapter.kt` and wired seek actions.
  - Added `timestamps_sidebar_toggle` in `exo_styled_player_control_view.xml`.
  - Integrated into `custom_exo_player_view_template.xml` and `PlayerFragment.kt` with SharedPreferences persistence.
- Successfully compiled and built Debug APK:
  - Output: `E:\Codes\Testing\LibreTube\app\build\outputs\apk\debug\app-debug.apk` (Size: 20.6 MB).
  - All Gradle/SDK/JDK caches stayed strictly on `E:\` without touching drive `C:`.
## Session 2026-09-11 (User Verification Success)
- User tested the Live In-Player Timestamps Sidebar APK on mobile.
- Confirmed that the feature works seamlessly (video continues playing uninterrupted while tapping timestamps to seek).
## Session 2026-09-11 (AI Smart Chapters & Turso Cloud Sync Complete)
- Fully ported and integrated the Chrome Extension (YT Smart Chapters Pro v3.0) into LibreTube:
  - Added `anime_wow.mp3` completion sound to `res/raw`.
  - Added `TranscriptHelper.kt` to extract VTT subtitles into timestamped transcripts.
  - Implemented `AiChaptersService.kt` with Qwen 3.8-max (AIKit) and OpenRouter support, automatic 30-minute chunking, and educational MCQ prompt.
  - Implemented `TursoSyncService.kt` for cloud database synchronization over libSQL HTTPS pipeline.
  - Created `AiSettingsSheet.kt` and `dialog_ai_settings.xml` for easy in-app configuration of API tokens and Turso credentials.
  - Updated `view_timestamps_sidebar.xml` with Category Filter Chips (All, ❓ Ques, 🎯 Option, 💡 Explain), AI Generate button, and DB Sync button.
  - Connected real-time seeking, chip filtering, and auto-load in `PlayerFragment.kt`.
- Built final APK: `E:\Codes\Testing\LibreTube\app\build\outputs\apk\debug\app-debug.apk` (Size: 24.5 MB).
## Session 2026-09-11 (TranscriptAPI Credentials Setting Update)
- Integrated TranscriptAPI.com into `TranscriptHelper.kt` using `https://transcriptapi.com/api/v2/youtube/transcript`.
- Added user-facing credential input box in `dialog_ai_settings.xml` and `AiSettingsSheet.kt` under "📜 TranscriptAPI.com Credentials".
- Configured user-manageable TranscriptAPI setting where users can enter their key anytime from in-app settings.
- Build succeeded in 1m (Incremental build).
- Verified output APK: `E:\Codes\Testing\LibreTube\app\build\outputs\apk\debug\app-debug.apk`.
## Session 2026-09-11 (Seekbar Markers, Navigation Buttons & AI SponsorBlock)
- Added forward (`timestamp_next`) and backward (`timestamp_prev`) buttons in `exo_styled_player_control_view.xml` directly next to `fullscreen` in `exo_basic_controls`.
- Added dynamic seekbar timestamp marker rendering in `ChapterTimeBar.kt` (`setTimestampMarkers()`, draws `#FFD54F` accent dots along progress bar).
- Ported Chrome extension's `analyzeRemovableSegments` AI prompt into `AiChaptersService.kt` to detect Sponsor, Intro, Outro, Selfpromo, Filler, and Tangents.
- Added `btn_ai_sponsorblock` ("🚫 Scan SB") to `view_timestamps_sidebar.xml`.
- Connected marker updates, prev/next timestamp seeking, and AI SponsorBlock segment drawing into `PlayerFragment.kt`.
- Successfully compiled and built Debug APK: `E:\Codes\Testing\LibreTube\app\build\outputs\apk\debug\app-debug.apk` (Size: 24.5 MB).
## Session 2026-09-11 (Choice-Wise Navigation Filter & App Version Bump to v32.2)
- Added choice-wise category filter dropdown button (`timestamp_filter`) in `exo_styled_player_control_view.xml`.
- Integrated `PopupMenu` in `PlayerFragment.kt` allowing users to select:
  - 📌 All Timestamps
  - ❓ Questions Only
  - 🎯 Options / Answers Only
  - 💡 Explanations Only
- Seekbar markers (`ChapterTimeBar`) and forward/backward buttons now dynamically filter according to the active category.
- Bumped app version in `app/build.gradle.kts`:
  - `versionCode`: 72 -> 73
  - `versionName`: "32.1" -> "32.2"
- Successfully compiled and built Debug APK: `E:\Codes\Testing\LibreTube\app\build\outputs\apk\debug\app-debug.apk`.
## Session 2026-09-11 (Smart Libre Brand, Zero Secrets, New GitHub Repo & Release v1.0.0)
- Renamed application to "Smart Libre" (`Smart Libre Debug` for debug build).
- Sanitized 100% of hardcoded API keys: removed literal keys from `TranscriptHelper.kt`, `dialog_ai_settings.xml`, and session docs. Zero secrets confirmed via deep grep.
- Changed app version to independent custom line:
  - `versionCode`: 10001
  - `versionName`: "1.0.0-smartchapters"
- Built signed, R8-optimized production Release APK: `SmartLibre-v1.0.0.apk` (Size: 8.88 MB).
- Created brand new public repository on GitHub: `https://github.com/pavnxet/Smart-Libre`.
- Pushed full codebase to `smartlibre/master`.
- Published official GitHub Release `v1.0.0` with `SmartLibre-v1.0.0.apk` asset attached.
## Session 2026-09-12 (Custom SponsorBlock User ID & Release v1.0.1 - Fixes #1)
- Added SponsorBlock User ID input to `dialog_ai_settings.xml` and wired to `PreferenceHelper.getSponsorBlockUserID()` in `AiSettingsSheet.kt`.
- Updated `AiChaptersService.kt` to bind the user's custom User ID to all AI-scanned segments.
- Added direct SponsorBlock upload confirmation dialog to `PlayerFragment.kt` after scanning segments.
- Bumped app version in `app/build.gradle.kts`:
  - `versionCode`: 10001 -> 10002
  - `versionName`: "1.0.0-smartchapters" -> "1.0.1-smartchapters"
- Built signed production Release APK: `SmartLibre-v1.0.1.apk`.
- Pushed changes to GitHub repository `pavnxet/Smart-Libre` and published Release `v1.0.1`.
- Closed Issue #1.

## Session 2026-09-12 (30-Minute Safe Chunking for AI SponsorBlock & Release v1.0.2)
- Added sequential 30-minute time-bucket chunking to `analyzeRemovableSegments()` in `AiChaptersService.kt`.
- Transcripts exceeding 30 minutes are now divided into bucketed slices to protect from LLM API context timeouts and rate limiting.
- Segments from all sequential chunks are merged and deduplicated.
- Bumped app version in `app/build.gradle.kts`:
  - `versionCode`: 10002 -> 10003
  - `versionName`: "1.0.1-smartchapters" -> "1.0.2-smartchapters"
- Built signed production Release APK: `SmartLibre-v1.0.2.apk`.
- Pushed changes to GitHub repository `pavnxet/Smart-Libre` and published Release `v1.0.2`.

## Session 2026-09-26 (ApiBeam Dynamic API Integration & Anime Wow Audio Engine Fix v1.0.3)
- Integrated ApiBeam provider (`PROVIDER_APIBEAM`) into `AiChaptersService.kt`:
  - Added support for customizable dynamic base URLs (`KEY_APIBEAM_URL`), supporting active time-based relay room endpoints.
  - Implemented standard OpenAI Chat Completions protocol (`/chat/completions`) with optional token handling.
  - Sliced educational MCQs and 30-min chunked SponsorBlock scanning directly through ApiBeam browser relay.
- Fixed Anime Wow completion sound:
  - Created `SoundHelper.kt` utilizing explicit `AudioAttributes` (`USAGE_MEDIA` / `CONTENT_TYPE_SONIFICATION`) and `openRawResourceFd`.
  - Added a dedicated 🔊 Test sound button inside the AI Settings sheet for immediate verification.
  - Wired sound playback to trigger on both AI Smart Chapters and AI SponsorBlock scan completions.
- Updated `dialog_ai_settings.xml` and `AiSettingsSheet.kt`:
  - Added ApiBeam radio selection with dynamic URL input field.
  - Added `NestedScrollView` to prevent bottom sheet clipping on smaller mobile displays.
- Bumped app version in `app/build.gradle.kts`:
  - `versionCode`: 10003 -> 10004
  - `versionName`: "1.0.2-smartchapters" -> "1.0.3-smartchapters"
- Built signed production Release APK: `SmartLibre-v1.0.3.apk`.
- Pushed changes to GitHub repository `pavnxet/Smart-Libre` and published Release `v1.0.3`.

## Session 2026-09-26 (API Chat Refresh Button & Out-of-Band Thread Isolation Protocol v1.0.4)
- Implemented Out-of-Band New-Chat Synchronization Protocol (NBCP) into `AiChaptersService.kt`:
  - Added `triggerNewChat(context)` method targeting local signaling relays (`/api/trigger-new-chat`, `/trigger-new-chat`) and ApiBeam endpoints directly (`/new-chat`, `/reset`).
  - Added configurable `KEY_RELAY_URL` (`DEFAULT_RELAY_URL = "http://localhost:3000"`).
- Added in-player chat refresh button (`btn_refresh_ai_chat`, `ic_refresh.xml`) to `view_timestamps_sidebar.xml` right next to settings and edit icons.
- Added "🔄 Refresh / Start New Chat on Web" button and relay URL field in `AiSettingsSheet.kt` and `dialog_ai_settings.xml`.
- Connected instant user notifications and fallback handling when signaling new chat sessions.
- Bumped app version in `app/build.gradle.kts`:
  - `versionCode`: 10004 -> 10005
  - `versionName`: "1.0.3-smartchapters" -> "1.0.4-smartchapters"
- Built signed production Release APK: `SmartLibre-v1.0.4.apk`.
- Pushed changes to GitHub repository `pavnxet/Smart-Libre` and published Release `v1.0.4`.







