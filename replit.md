# Audio Quran on Replit

This repository is a native Android/Kotlin app, not a web server. Replit's browser preview cannot run the app; build an APK here and install it on an Android device or use an external Android emulator.

## Debug build

The workspace uses JDK 21 and an Android SDK at `.local/android-sdk/` (ignored by Git). The SDK has Android platform 36.1 and build tools 36.1.0; Gradle may also download build tools 36.0.0 as required by the Android Gradle plugin. From the repository root:

```sh
ANDROID_HOME="$PWD/.local/android-sdk" bash gradlew :app:assembleDebug --console=plain
```

The resulting APK is `app/build/outputs/apk/debug/app-debug.apk`. It installs as **MP3 Quran Preview**, separately from the original `MP3 Quran` app. This is deliberate: the bundled `MP3-Quran.apk` and the debug APK have different signing certificates, so the debug build cannot update the original install without its original signing key. Keep the original installed if it contains saved data; the preview starts with its own empty local data. Android's default debug keystore is created automatically; no release signing credentials are needed for this build. The Gradle wrapper downloads the project's Gradle version and dependencies on first use.

## Android playback smoke test

On September 29, 2026, the debug APK was installed on an Android 9 (API 28) x86_64 emulator. The device test streamed Surah 112 from the default reciter, downloaded its MP3, checked the saved file and database entry, then played it from the saved file while its network URL was deliberately unreachable. All three checks passed; no playback or download runtime issue was observed on that emulator. This does not replace testing on a physical device or newer Android versions.

To repeat on a connected Android device or emulator:

```sh
ANDROID_HOME="$PWD/.local/android-sdk" bash gradlew :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
.local/android-sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
.local/android-sdk/platform-tools/adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
.local/android-sdk/platform-tools/adb shell am instrument -w -r -e class com.example.AudioDownloadSmokeTest com.aistudio.audioquran.mpquran.preview.test/androidx.test.runner.AndroidJUnitRunner
```

The test needs internet access to download the live MP3 and deletes its test download afterward. If Android rejects instrumentation because signatures differ, rebuild and reinstall **both** APKs with the same debug signing key (uninstall a previously signed app first if necessary).

If the ignored SDK directory is missing in a fresh environment, install the Android command-line tools and use `sdkmanager` to install `platforms;android-36.1` and `build-tools;36.1.0`, accepting the Android SDK licenses first. Then run the command above.

## External configuration

The sample `.env.example` mentions a Gemini API key, but the setting is commented out and the debug build succeeds without it. Do not put real keys in tracked files. Google Services also reports that `google-services.json` is absent; the debug build succeeds without it, but any feature that requires a configured Firebase project needs its own configuration. Release builds additionally require the signing keystore and `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` environment variables.