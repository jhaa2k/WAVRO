# WAVRO Studio

WAVRO Studio is a native Android audio-visualization app built with Kotlin and Jetpack Compose.
The Android project follows the `com.wavro.studio` package structure and lives in `app/`.

## Features

- Import and play local audio through Android's system file picker.
- Play a built-in generated demo track.
- Render a live spectrum, radial ring, and particle preview.
- Add, enable, and remove visualizer elements.
- Save, reopen, and delete projects on-device.
- Request audio-analysis permission only when live audio visualization is enabled.

Audio stays on the device. WAVRO does not upload selected tracks.

## Build

Requirements: JDK 17 and Android SDK Platform 35.

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Install the debug APK on a connected device:

```bash
./gradlew :app:installDebug
```

The Android build workflow runs the unit tests and assembles an APK for pushes and pull requests.

## Project layout

```text
app/src/main/java/com/wavro/studio/
├── app/          Application setup
├── audio/        Playback, audio analysis, spectrum mapping
├── domain/       Project and scene models
├── editor/       Studio state and actions
├── projects/     On-device project persistence
├── rendering/    Audio-reactive preview
└── ui/           Jetpack Compose screens
```

The previous browser prototype is retained under `web-prototype/` as a reference; the Android app is the primary application.
