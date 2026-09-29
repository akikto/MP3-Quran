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

## Play Store release bundle

For a **new** Play Console listing, use `com.aistudio.audioquran.mpquran` as the application ID. The existing debug APK uses a different `.preview` ID and debug signing key; do not upload it to Play. Version code 1 is suitable for the first release; increment it before every subsequent release.

Save a strong password as the `STORE_PASSWORD` Replit Secret. Generate the upload key once, then build the signed release Android App Bundle:

```sh
bash scripts/create-upload-key.sh
ANDROID_HOME="$PWD/.local/android-sdk" bash gradlew :app:bundleRelease --no-daemon --console=plain
```

The bundle is at `app/build/outputs/bundle/release/app-release.aab`. The existing download workflow also serves it directly at `/MP3-Quran-Release.aab` with the `.aab` filename; the workspace asset card may repackage binary downloads. The upload key is `.local/signing/mp3-quran-upload.p12`, ignored by Git, with alias `upload`. **Back up the key file outside this workspace and its password in a separate secure place.** Do not commit or share either one. This key identifies future uploads; Google Play App Signing handles the key used to sign the APKs delivered to users. If this workspace is reset without a backup, a new key will require a Play Console upload-key reset before updates can be submitted. If using an existing upload key instead, set `KEYSTORE_PATH`, `STORE_PASSWORD`, and (only if different) `KEY_PASSWORD` as environment settings/secrets; it must contain the `upload` alias.

Before uploading, complete the Play Console store listing, privacy policy, Data safety and app-content declarations, and confirm distribution rights for the recitation streams provided by mp3quran.net. The app declares a `mediaPlayback` foreground service for background audio, which requires a matching Play Console declaration and demonstration video. Upload the release AAB to a testing track first. If the developer account is a new personal account, Google Play currently requires at least 12 closed testers opted in continuously for 14 days before applying for production access.

## External configuration

The sample `.env.example` mentions a Gemini API key, but the setting is commented out and the debug build succeeds without it. Do not put real keys in tracked files. Firebase is not currently configured or used; enabling it later requires real, variant-appropriate project configuration. Release builds require the upload keystore and `STORE_PASSWORD` Replit Secret. `KEYSTORE_PATH` is optional when using the default key path above; `KEY_PASSWORD` is optional when it matches `STORE_PASSWORD`.

## GitHub pushes

`origin` fetches over HTTPS and pushes over SSH using a write-enabled **deploy key scoped to this repository**. The private key is outside the repository at `/home/runner/.ssh/mp3-quran-deploy`; Git's local `core.sshCommand` selects it and requires a verified GitHub host key. Never copy the private key into Git, logs, or a chat message. The GitHub connector's REST API can sync file snapshots, but does **not** upload the actual local Git commits; use `git push origin main` instead.

Before pushing, run `bash scripts/check-github-main.sh`. It compares the local and live GitHub `main` commit and tree hashes and verifies that the remote commit is an ancestor of local `main`. The check reads remote history into a temporary repository and does not update GitHub or local refs. If the remote is not an ancestor, reconcile the branches before pushing; never force-push. After pushing, compare `git rev-parse main` with `git ls-remote git@github.com:akikto/MP3-Quran.git refs/heads/main`, and `git rev-parse main^{tree}` with `git rev-parse origin/main^{tree}` (fetch again if needed). Matching commits also prove matching history; matching trees prove matching tracked files.

The key and Git configuration are local to this workspace, not tracked in the repository. If this workspace is recreated, generate a new SSH key outside the repository, register **only its public key** as a writable repository deploy key, verify GitHub's SSH host fingerprint, and configure `remote.origin.pushurl` and `core.sshCommand` again. Remove the old deploy key from GitHub when it is no longer needed.

### Recovering GitHub push access after a workspace reset

Assume the old private key and local Git settings are gone; do not copy a private key into the repository or chat. Create a replacement outside the repository:

```sh
install -d -m 700 /home/runner/.ssh
umask 077
ssh-keygen -t ed25519 -N '' -C 'MP3-Quran Replit deploy key' -f /home/runner/.ssh/mp3-quran-deploy
chmod 600 /home/runner/.ssh/mp3-quran-deploy
```

In GitHub, open **akikto/MP3-Quran → Settings → Deploy keys → Add deploy key**. Paste only the output of `cat /home/runner/.ssh/mp3-quran-deploy.pub`, enable **Allow write access**, and save. Keep the old deploy key until a push with the replacement has succeeded.

Pin GitHub's published Ed25519 host key and confirm its fingerprint before using SSH:

```sh
printf '%s\n' 'github.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl' > /home/runner/.ssh/known_hosts
chmod 600 /home/runner/.ssh/known_hosts
ssh-keygen -lf /home/runner/.ssh/known_hosts -E sha256
```

The fingerprint must be `SHA256:+DiY3wvvV6TuJJhbpZisF/zLDA0zPMSvHdkr4UvCOqU`. Configure push-only SSH while keeping HTTPS fetch:

```sh
git config --local remote.origin.pushurl 'git@github.com:akikto/MP3-Quran.git'
git config --local core.sshCommand 'ssh -i /home/runner/.ssh/mp3-quran-deploy -o IdentitiesOnly=yes -o UserKnownHostsFile=/home/runner/.ssh/known_hosts -o StrictHostKeyChecking=yes'
```

Fetch before pushing. If `git merge-base --is-ancestor origin/main main` fails, reconcile the histories first and never force-push. After a normal `git push origin main`, confirm the remote commit and tree match local `main`; only then delete the obsolete deploy key in the repository's **Settings → Deploy keys**.
