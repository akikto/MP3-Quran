---
name: GitHub connector sync
description: How to safely sync a GitHub repository when Git CLI authentication fails but the connector works
---

The GitHub connector can write through the Git Data API even when the workspace's Git CLI credentials are rejected. An API-created sync commit transfers the current files but does not preserve the local commit history. Verify the resulting remote tree matches the local Git tree, then merge the remote commit into local history so subsequent pushes can fast-forward.

**Why:** Connector authorization did not repair Git CLI authentication. The Git Data API accepted the same file tree, but a sync commit created a different history.

**How to apply:** Never force-update the remote branch. Check its current ref before updating, compare complete tree hashes afterward, and reconcile local history with the remote commit. For binary uploads, verify each uploaded blob SHA against Git's hash: large shell callback output may be silently shortened, and shell output may normalize tab and line endings. Use temporary base64 files and bounded file reads rather than trusting large shell output.