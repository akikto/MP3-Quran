# Audio Quran on Replit

This repository is a native Android/Kotlin app, not a web server. Replit's browser preview cannot run the app; build an APK here and install it on an Android device or use an external Android emulator.

## Debug build

The workspace uses JDK 21 and an Android SDK at `.local/android-sdk/` (ignored by Git). The SDK has Android platform 36.1 and build tools 36.1.0; Gradle may also download build tools 36.0.0 as required by the Android Gradle plugin. From the repository root:

```sh
ANDROID_HOME="$PWD/.local/android-sdk" bash gradlew :app:assembleDebug --console=plain
```

The resulting APK is `app/build/outputs/apk/debug/app-debug.apk`. It installs as **MP3 Quran Preview**, separately from the original `MP3 Quran` app. This is deliberate: the bundled `MP3-Quran.apk` and the debug APK have different signing certificates, so the debug build cannot update the original install without its original signing key. Keep the original installed if it contains saved data; the preview starts with its own empty local data. Android's default debug keystore is created automatically; no release signing credentials are needed for this build. The Gradle wrapper downloads the project's Gradle version and dependencies on first use.

The `Download Preview APK` workflow serves the built file directly at `/MP3-Quran-Preview.apk` with the Android APK content type and filename. Use that link to download onto a phone; the workspace asset card repackages unknown binary file types as `.zip`, which cannot be installed directly.

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


## Collections file-picker restore check

The instrumented `CollectionsDocumentPickerTest` drives Android's real Downloads document provider from the Playlists screen. It saves a JSON document, clears Room tables to simulate an empty installation, imports the document through the picker, and checks the restored favorite Surah, ayah bookmark, playlist description, and item order in the database and UI. It also checks picker cancellation and importing an empty/unreadable JSON file. **Run only on a disposable emulator**: the restore test deletes the preview app's local database content. It cannot call `pm clear` from within instrumentation because that kills the test process.

```sh
ANDROID_HOME="$PWD/.local/android-sdk" bash gradlew :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
.local/android-sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
.local/android-sdk/platform-tools/adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
.local/android-sdk/platform-tools/adb shell am instrument -w -r -e class com.example.CollectionsDocumentPickerTest com.aistudio.audioquran.mpquran.preview.test/androidx.test.runner.AndroidJUnitRunner
```

For a separate **full app-data reset** check, first create a favorite Surah, an ayah bookmark, and a custom playlist with a description and multiple Surahs in the preview app. Tap Playlists → Export saved and save the file in Downloads. Run `.local/android-sdk/platform-tools/adb shell pm clear com.aistudio.audioquran.mpquran.preview` (this deletes all local preview app data), reopen the app, tap Playlists → Import saved, select the file from Downloads, and check Saved and Playlists for those items and their order. Also try Back from the picker and selecting an empty `.json` document: cancellation should leave collections unchanged, while an unreadable file should show “Import failed” without importing anything. Built-in starter collections may appear on first launch and are not part of the exported personal backup.

On September 29, 2026, the full `pm clear` procedure was also run on an Android 9 emulator: the previously exported Downloads JSON was selected through the system picker after clearing the preview app, and its favorite Surah, ayah bookmark, playlist name, description, and item order were verified in the restored database. The three repeatable picker instrumentation tests passed on the same emulator.

If the ignored SDK directory is missing in a fresh environment, install the Android command-line tools and use `sdkmanager` to install `platforms;android-36.1` and `build-tools;36.1.0`, accepting the Android SDK licenses first. Then run the command above.

## External configuration

The sample `.env.example` mentions a Gemini API key, but the setting is commented out and the debug build succeeds without it. Do not put real keys in tracked files. Google Services also reports that `google-services.json` is absent; the debug build succeeds without it, but any feature that requires a configured Firebase project needs its own configuration. Release builds additionally require the signing keystore and `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` environment variables.

## GitHub pushes

`origin` fetches over HTTPS and pushes over SSH using a write-enabled **deploy key scoped to this repository**. The private key is outside the repository at `/home/runner/.ssh/mp3-quran-deploy`; Git's local `core.sshCommand` selects it and requires a verified GitHub host key. Never copy the private key into Git, logs, or a chat message. The GitHub connector's REST API can sync file snapshots, but does **not** upload the actual local Git commits; use `git push origin main` instead.

Before pushing, `git fetch origin` and confirm `git merge-base --is-ancestor origin/main main` succeeds. If it does not, reconcile the branches before pushing; never force-push. After pushing, compare `git rev-parse main` with `git ls-remote git@github.com:akikto/MP3-Quran.git refs/heads/main`, and `git rev-parse main^{tree}` with `git rev-parse origin/main^{tree}` (fetch again if needed). Matching commits also prove matching history; matching trees prove matching tracked files.

The key and Git configuration are local to this workspace, not tracked in the repository. If this workspace is recreated, generate a new SSH key outside the repository, register **only its public key** as a writable repository deploy key, verify GitHub's SSH host fingerprint, and configure `remote.origin.pushurl` and `core.sshCommand` again. Remove the old deploy key from GitHub when it is no longer needed.
