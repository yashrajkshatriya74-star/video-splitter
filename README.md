# Video Splitter Android App

A simple offline Android video splitter designed for AI-video workflows.

## Features
- Pick videos through the Android document picker.
- MP4, MOV and any other video containers/codecs supported by the device's Android MediaExtractor.
- Split every N seconds (for example 5, 10, 15, 30, 60).
- Split into an equal number of parts.
- Keeps original encoded video/audio streams; no re-encoding.
- Saves MP4 clips under `Movies/VideoSplitter` on Android 10+.
- Preserves video rotation metadata where supported.

## Important
Because this version does not re-encode, a requested split can start at the nearest video keyframe. This is extremely fast and avoids quality loss, but a split is not always frame-exact. For exact frame-accurate cuts, a future FFmpeg/re-encoding mode can be added.

## Build
Open the project in Android Studio with an installed Android SDK and let Gradle sync. Then Build > Build APK(s).
