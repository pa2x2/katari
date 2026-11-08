---
name: sync-server-release-notes
description: Write the sync server's release notes in features/sync/server/CHANGELOG.md. Run through the release-notes skill, which decides what a Katari release covers and holds the writing rules.
---

Follow the release-notes skill (`.agents/skills/release-notes/SKILL.md`) for what the release covers and for its
changelog writing rules; this skill adds what only the sync server's changelog needs.

The server's audience is the people who run it: they pull its image, set its environment variables, keep its data
volume, and manage accounts on its web page. Its notes are the only notes they get, since a server release publishes an
image to the repository's packages and makes no GitHub release.

## Inspect the release

1. Read `version` from `features/sync/server/build.gradle.kts` as the release version: a semantic version, which may
   carry a pre-release suffix such as `0.2.0-beta.1`, published under its exact tag only. Derive future tag
   `sync-server-v<version>`; stop if the configured version is missing or is not a semantic version.
2. Find the last server release: the highest `sync-server-v*` tag (`git tag --list 'sync-server-v*' --sort=-v:refname`).
   Inspect `git log <tag>..HEAD` and `git diff <tag>...HEAD` for outcomes the people running the server notice:
   `features/sync/server/` (its API, web page, storage, retention and recovery command), `features/sync/protocol/` as
   the server serves it, and `features/sync/server/Dockerfile`. The apps' sync code (`features/sync/client/` and the
   apps' sync screens) belongs to the apps' changelogs, not this one.
3. With no `sync-server-v*` tag yet, this is the server's first release: there is no earlier version to compare with,
   so describe what the server offers at `HEAD` in a few `✨ Added` outcomes instead of listing changes, and ask the
   user to confirm them before writing.

## What its notes must also say

Beyond the shared rules, these changes always reach the server's notes, because missing them breaks someone's setup:

- The range of protocol versions it accepts (`SupportedProtocolVersions` in
  `features/sync/server/.../http/ProtocolVersionCheck.kt`) moving, as the oldest and newest app versions it works with.
  Name the Android and desktop app versions, not protocol numbers.
- Environment variables added, removed, renamed, or given a new default; the port, the data directory or its layout,
  the user the image runs as, or the base image's Java version changing.
- A change to the server's database that older server versions can't open, so going back to them needs a copy of the
  data volume taken before upgrading.

## Update features/sync/server/CHANGELOG.md

1. Update `features/sync/server/CHANGELOG.md` for the configured version using the shared writing rules. Keep its own
   `[Unreleased]` link, comparing the target `sync-server-v<version>` tag with `HEAD`, and add or update the target
   version's link to its tag, `https://github.com/pa2x2/katari/tree/sync-server-v<version>`, since there is no release
   page.
2. Leave Mihon out: Mihon has no sync server.
