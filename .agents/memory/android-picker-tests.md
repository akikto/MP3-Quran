---
name: Android picker instrumentation
description: Constraints for repeatable real DocumentsUI tests on a disposable emulator
---

Use a disposable emulator for end-to-end document-picker tests that reset local app data. Do not invoke package-wide clear from an instrumented process; it kills the process running the test. Clear Room data inside the test, and use an outside-the-process manual or scripted check when package-wide clearing matters.

**Why:** A test cannot survive clearing its own package; the debug preview may also contain user data on a shared device.

**How to apply:** Restrict destructive instrumentation to disposable emulators and clearly warn before running it. Give exported documents unique names so repeated runs do not accidentally select a prior file. Keep picker test shell commands simple: a multi-command string with redirection passed to UiDevice's shell-command API created a directory with the intended filename instead of a file in this environment.