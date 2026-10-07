# Soundwave

A Kotlin + Jetpack Compose Android music-player recruitment task project. It includes a hardcoded five-song demo playlist, a Now Playing screen, a bundled CC0 MP3 with real Play/Pause controls, a searchable iTunes catalog, and background playback through a Media3 foreground service.

## Build and run

1. Open this folder in Android Studio, or run `gradlew.bat assembleDebug` from PowerShell.
2. Use JDK 17 and install Android SDK Platform 36 (or a newer compatible platform).
3. Run the `app` configuration on an emulator or Android phone. An internet connection is needed to load the catalog; the bundled track works offline.

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## App behavior

- The first five songs are hardcoded so the home screen is populated without network access.
- The first playlist track is bundled and playable offline. The remaining hardcoded entries are UI/navigation examples; they intentionally do not pretend to have audio files.
- Search the iTunes Store catalog for live track names, artists, artwork, and store links. API results are informational and link to their store page; their Apple-provided audio previews are not used for in-app playback.
- Media3 owns playback in a `MediaSessionService`, allowing the bundled track and system media controls to continue when the activity is minimized or the screen is off.
- Notification permission is requested on Android 13 and later so the media notification can be shown.

## Music license

`app/src/main/res/raw/merfolk.mp3` is **“The Merfolk I Should Turn To Be” by Soft and Furious**, track 7 from the album of the same name. The recording is marked **CC0 1.0 (Public Domain Dedication)** in the Internet Archive item metadata:

- [Internet Archive item and license metadata](https://archive.org/details/SoftAndFuriousTheMerfolkIShouldTurnToBe)
- [Creative Commons CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/)
- Source file: `Soft and Furious - The Merfolk I Should Turn To Be - 07 The Merfolk I Should Turn To Be.mp3`

CC0 does not require attribution; the artist and source are credited here as a courtesy.

## Music catalog

Catalog data is fetched from the public [iTunes Search API](https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/iTuneSearchAPI/index.html). It requires no API key. Catalog metadata and artwork are used to promote the linked store content; the app does not play or redistribute the catalog audio previews.

## Submission

Build a release APK with `gradlew.bat assembleRelease` after configuring a release signing key in Android Studio. Do not commit signing keys or passwords. Publish this project to a public GitHub repository and submit its URL and APK through the recruitment form.
