---
name: Legacy favorite provenance
description: Why existing favorites are treated as personal when introducing starter provenance
---

Existing database favorites cannot reliably be distinguished from seeded favorites. Treat all pre-provenance favorites as user-set, even if some originated as starters.

**Why:** A starter Surah can also be a user's chosen favorite, and timestamps or Surah numbers do not prove origin. Guessing could overwrite a user's saved date during restore.

**How to apply:** When extending restore or database upgrades, only replace dates for favorites explicitly marked as untouched starters on newly created databases. Preserve dates for legacy rows with unknown origin.