# Audio Quran on Replit

This repository is a native Android/Kotlin app, not a web server. Replit's browser preview cannot run the app; build an APK here and install it on an Android device or use an external Android emulator.

## Debug build

The workspace uses JDK 21 and an Android SDK at `.local/android-sdk/` (ignored by Git). The SDK has Android platform 36.1 and build tools 36.1.0; Gradle may also download build tools 36.0.0 as required by the Android Gradle plugin. From the repository root:

```sh
ANDROID_HOME="$PWD/.local/android-sdk" bash gradlew :app:assembleDebug --console=plain
```

The resulting APK is `app/build/outputs/apk/debug/app-debug.apk`. Android's default debug keystore is created automatically; no release signing credentials are needed for this build. The Gradle wrapper downloads the project's Gradle version and dependencies on first use.

If the ignored SDK directory is missing in a fresh environment, install the Android command-line tools and use `sdkmanager` to install `platforms;android-36.1` and `build-tools;36.1.0`, accepting the Android SDK licenses first. Then run the command above.

## External configuration

The sample `.env.example` mentions a Gemini API key, but the setting is commented out and the debug build succeeds without it. Do not put real keys in tracked files. Google Services also reports that `google-services.json` is absent; the debug build succeeds without it, but any feature that requires a configured Firebase project needs its own configuration. Release builds additionally require the signing keystore and `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` environment variables.