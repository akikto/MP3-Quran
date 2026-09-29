---
name: GitHub connector sync
description: Why repository-scoped SSH Git pushes replace connector API snapshot syncs
---

Prefer a repository-scoped, writable SSH deploy key for Git pushes over the connector's Git Data API. Keep the private key outside the tracked repository and never expose it to the agent context. The connector can authorize registering the *public* deploy key without revealing its own token, subject to the workflow-file restriction below. Its Git Data API remains a fallback for file snapshots only: API-created sync commits transfer current files but do not preserve local commit history.

When pushing commits that change GitHub Actions workflow files, do not register the deploy key through an OAuth app that lacks the `workflow` scope. GitHub attributes that key to the app and rejects the push even if the key has write access. Register the public key directly in the repository's Deploy keys settings instead.

**Why:** A normal SSH push carrying a workflow change was rejected with “refusing to allow an OAuth App to create or update workflow ... without `workflow` scope” after the deploy key was created through the GitHub connector. The connector's offered scopes did not include `workflow`.

**How to apply:** Use the connector to inspect permissions if needed, but for workflow-changing pushes have the repository owner add the public key via GitHub's settings with write access. Keep the private key outside the repository and verify the resulting push preserves commit and tree hashes.

For a read-only check of live Git history, fetch into a separate temporary repository rather than the active checkout. A source-only fetch refspec can still apply `remote.origin.fetch` and move a local tracking ref; using a temporary object directory alone can leave that ref pointing at deleted objects.

**Why:** A source-only fetch in the active checkout advanced its tracking ref even though no destination ref was given. Cleaning the temporary object directory then left that local ref unreadable.

**How to apply:** Keep inspection fetches isolated from the active checkout's refs and object cleanup. Suppress raw Git error output when a remote URL or helper could contain credentials, and report a generic failure instead.

**Why:** Connector authorization did not repair Git CLI authentication. The Git Data API accepted the same file tree, but a sync commit created different history. A real SSH Git push preserved the exact local commit and tree hashes without force-updating GitHub.

**How to apply:** For routine sync, fetch first and check that the remote branch is an ancestor, then use a normal Git push and compare the remote commit and tree hashes. Never force-update. If a recreated workspace lacks the untracked private key, provision a new repository deploy key and remove the old one, rather than reverting silently to snapshot commits. For any necessary API binary uploads, verify each uploaded blob SHA against Git's hash: large shell callback output may be silently shortened, and shell output may normalize tab and line endings.