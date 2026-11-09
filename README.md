# AUDIOVIS Studio Pro — Native Android

A native Kotlin + C++ Android project foundation based on the supplied `index.html` and `styles.css`.

## Current status

This is **Milestone 1: Android project foundation and Compose UI shell**. It is not yet a feature-complete conversion.

Implemented in this starter:
- Native Android application module using Kotlin and Jetpack Compose
- Dark AUDIOVIS-style studio shell and six navigation destinations
- Preview placeholder, transport controls and waveform placeholder
- CMake/NDK native library target and JNI bridge
- Starter radix-2 FFT source
- GitHub Actions workflow to build and upload a debug APK

Not yet implemented:
- Actual audio file selection and Media3 playback/session notification
- Real PCM capture and FFT-driven visualization
- Original JavaScript layer, preset, scene and settings behavior
- Full OpenGL ES visualizer and video compositor
- MediaCodec/MediaMuxer export pipeline
- Room project persistence and DataStore preferences
- Full theme catalog, gestures, undo/redo and editing operations

The uploaded HTML references `app.js`, but that file was not included. Exact functional parity cannot be completed from HTML and CSS alone.

## Build on GitHub

1. Create a repository named `AUDIOVIS-STUDIO-PRO`.
2. Upload the contents of this project ZIP to the repository root.
3. Commit to the `main` branch.
4. Open **Actions** and select **Build AUDIOVIS Studio Pro APK**.
5. Run the workflow (or let the push trigger it).
6. Open the completed workflow run and download the `AUDIOVIS-Studio-Pro-debug` artifact.

The artifact contains `app-debug.apk`. It is a debug build, not a Play Store release.

## Build requirements

- JDK 17
- Android SDK platform 35
- Android NDK 27.2.12479018
- CMake 3.22.1
- Gradle 8.11.1
- Android Gradle Plugin 8.9.2

## Next required source

Upload the original `app.js` and any local assets used by the web project. They are needed to map the original interactions, controls, settings, presets and export behavior into native Kotlin modules.
