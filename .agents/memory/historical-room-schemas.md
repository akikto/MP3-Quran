---
name: Historical Room schemas
description: Migration policy when older database schemas cannot be verified
---

Do not invent migrations for unknown older database layouts; leave destructive fallback disabled so an unsupported version fails without deleting saved user data.

**Why:** The available project history did not include exported schemas for releases before version 3. A guessed migration could silently damage a user's saved data.

**How to apply:** Before adding another historical migration, obtain an actual older release database or schema and test the upgrade against it. Keep exporting schemas for all future versions.