# Japanese Grammar App

Android app for Japanese grammar analysis, OCR-assisted text capture, bookmarks, history, flashcards, and configurable LLM/TTS providers.

## Tech Stack

- Kotlin and Jetpack Compose
- Hilt dependency injection
- Room database with schema exports
- Retrofit/OkHttp networking
- ML Kit OCR and ONNX Runtime for local text region detection

## Requirements

- Android Studio with JDK 17
- Android SDK 34
- Gradle wrapper included in this repository

## Build

```powershell
.\gradlew.bat assembleDebug
```

Run unit tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

## Release Signing

Release signing values are intentionally kept out of Git. Configure them locally through `local.properties`, Gradle properties, or environment variables:

```properties
RELEASE_STORE_FILE=release.jks
RELEASE_STORE_PASSWORD=your-store-password
RELEASE_KEY_ALIAS=your-key-alias
RELEASE_KEY_PASSWORD=your-key-password
```

The keystore file and release APKs should stay local. Publish built APKs through GitHub Releases or another distribution channel instead of committing them to source control.

## Notes

- API keys are entered in app settings and stored locally on the device.
- `local.properties`, signing keys, APKs, IDE state, prompt backups, and temporary scrape/search files are ignored by Git.
- The bundled OCR model under `app/src/main/assets/ocr/rapidocr/` is required for the RapidOCR text region detector.
