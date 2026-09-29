#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
key_path="$PWD/.local/signing/mp3-quran-upload.p12"

if [[ -e "$key_path" ]]; then
  echo "Upload key already exists; refusing to replace it: $key_path" >&2
  exit 1
fi

if [[ -z "${STORE_PASSWORD:-}" ]]; then
  echo "STORE_PASSWORD is required in Replit Secrets before generating an upload key." >&2
  exit 1
fi

umask 077
mkdir -p "$(dirname "$key_path")"
chmod 700 "$(dirname "$key_path")"

keytool -genkeypair \
  -keystore "$key_path" \
  -storetype PKCS12 \
  -alias upload \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=MP3 Quran Upload" \
  -storepass:env STORE_PASSWORD \
  -keypass:env STORE_PASSWORD \
  -noprompt

chmod 600 "$key_path"
echo "Upload key created at $key_path. Back up this file and its password separately."