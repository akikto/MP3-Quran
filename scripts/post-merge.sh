#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
export ANDROID_HOME="$PWD/.local/android-sdk"

if [[ ! -d "$ANDROID_HOME/platforms/android-36.1" ]]; then
  echo "Android SDK platform 36.1 is missing. See replit.md for setup instructions." >&2
  exit 1
fi

bash gradlew :app:assembleDebug --no-daemon --console=plain