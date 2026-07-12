---
description: Generate Katari release notes and update a draft release
---
Use the `release-notes` skill to prepare Katari release notes for version `$ARGUMENTS`.

Update `CHANGELOG.md` in the worktree, but do not commit it. Show the exact proposed
GitHub release body and require explicit user confirmation before running any command
that changes the GitHub release. Never publish a release, create or push a tag, upload
artifacts, or commit changes.
