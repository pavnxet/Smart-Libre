# LibreTube Execution Flow

## Build & Run Path
- `./gradlew.bat assembleDebug` builds the debug APK at `app/build/outputs/apk/debug/app-debug.apk`.
- `adb.exe install -r <apk-path>` deploys to connected Android devices.
