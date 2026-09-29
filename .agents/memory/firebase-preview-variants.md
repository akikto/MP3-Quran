---
name: Firebase preview variants
description: Avoid broken Android CI builds from mismatched Google Services package registrations
---

The Google Services plugin's missing-file warning does not cover a JSON file whose Android client package fails to match the build variant. A preview/debug application ID with a suffix is a distinct Firebase Android app from the release application ID.

**Why:** A dummy release-only Firebase configuration made the debug GitHub Actions build fail before APK compilation, despite a warning strategy for missing configuration files.

**How to apply:** If the app does not use Firebase, do not enable its plugin or manufacture a placeholder JSON just to satisfy the build. If Firebase is introduced later, register each actual application ID in Firebase and supply authentic variant-appropriate configuration instead.