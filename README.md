# Japanese Grammar App

Japanese Grammar App is an Android study tool for analyzing Japanese sentences with configurable LLM providers, OCR-assisted text capture, bookmarks, history, and flashcard review.

[Download the latest APK](https://github.com/m1kuk1m/JapaneseGrammarApp/releases/latest)

## Preview

<p>
  <img src="docs/screenshots/home.jpg" alt="Japanese Grammar App home screen" width="280">
  <img src="docs/screenshots/analysis-result.jpg" alt="Japanese Grammar App analysis result screen" width="280">
</p>

## Features

- Analyze Japanese sentences into grammar-focused explanations.
- Capture Japanese text from camera or image input with OCR support.
- Review saved sentences and segments with bookmarks and flashcards.
- Keep local analysis history for later study.
- Configure LLM and TTS providers from the app settings.
- Tune OCR text-region detection for different image layouts.

## For Users

Install the APK from the [latest release](https://github.com/m1kuk1m/JapaneseGrammarApp/releases/latest). Android may ask for permission to install apps from the browser or file manager used to open the APK.

The app requires API credentials for the LLM or TTS provider selected in settings. API keys are entered inside the app and stored locally on the device.

Release assets include a `.sha256` checksum file. After downloading the APK, users who want to verify the file can compare its SHA-256 hash with the checksum published on the release page.

## Tech Stack

- Kotlin and Jetpack Compose
- Hilt dependency injection
- Room database with schema exports
- Retrofit and OkHttp networking
- ML Kit OCR
- ONNX Runtime for local text-region detection

## For Developers

Requirements:

- Android Studio
- JDK 17
- Android SDK 34
- Gradle wrapper included in this repository

Build a debug APK on Windows:

```powershell
.\gradlew.bat assembleDebug
```

Run unit tests on Windows:

```powershell
.\gradlew.bat testDebugUnitTest
```

On macOS or Linux:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Release Builds

Release signing values must stay outside source control. Local release builds can provide signing values through `local.properties`, Gradle properties, or environment variables:

```properties
RELEASE_STORE_FILE=release.jks
RELEASE_STORE_PASSWORD=your-store-password
RELEASE_KEY_ALIAS=your-key-alias
RELEASE_KEY_PASSWORD=your-key-password
```

The keystore file, generated APKs, `local.properties`, IDE state, prompt backups, and temporary scrape/search files are ignored by Git.

The GitHub Actions release workflow can build and attach a signed APK when maintainers configure these repository secrets:

```text
RELEASE_KEYSTORE_BASE64
RELEASE_STORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

Create `RELEASE_KEYSTORE_BASE64` from a keystore:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks")) | Set-Clipboard
```

After updating `versionCode` and `versionName`, push a version tag such as `v1.0.1` to trigger the release workflow.

## Versioning

- Keep `versionName` aligned with GitHub release tags, for example `1.0.1` and `v1.0.1`.
- Increase `versionCode` before publishing an APK that should upgrade an installed copy.
- The current Android package name is `com.example.japanesegrammarapp`. Changing it will make Android treat the app as a different application.

## Third-Party Notices

The bundled OCR model under `app/src/main/assets/ocr/rapidocr/` is required for the RapidOCR text-region detector. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for model and dependency notes.

## License

This repository is publicly visible for reference and personal study. The project source and bundled assets are not open-source licensed for redistribution or reuse unless separate permission is granted. See [LICENSE](LICENSE).
