---
name: GitHub connector sync
description: Why repository-scoped SSH Git pushes replace connector API snapshot syncs
---

Prefer a repository-scoped, writable SSH deploy key for Git pushes over the connector's Git Data API. Keep the private key outside the tracked repository and never expose it to the agent context. The connector can authorize registering the *public* deploy key without revealing its own token. Its Git Data API remains a fallback for file snapshots only: API-created sync commits transfer current files but do not preserve local commit history.

**Why:** Connector authorization did not repair Git CLI authentication. The Git Data API accepted the same file tree, but a sync commit created different history. A real SSH Git push preserved the exact local commit and tree hashes without force-updating GitHub.

**How to apply:** For routine sync, fetch first and check that the remote branch is an ancestor, then use a normal Git push and compare the remote commit and tree hashes. Never force-update. If a recreated workspace lacks the untracked private key, provision a new repository deploy key and remove the old one, rather than reverting silently to snapshot commits. For any necessary API binary uploads, verify each uploaded blob SHA against Git's hash: large shell callback output may be silently shortened, and shell output may normalize tab and line endings.