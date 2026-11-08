---
name: desktop-release-notes
description: Write the desktop app's release notes in desktop/CHANGELOG.md. Run through the release-notes skill, which decides what a Katari release covers and holds the writing rules.
---

Follow the release-notes skill (`.agents/skills/release-notes/SKILL.md`) for what the release covers and for its
changelog writing rules; this skill adds what only the desktop app's changelog needs.

## Inspect the release

1. Read `version` from `desktop/build.gradle.kts` as the release version: a semantic version, which may carry a
   pre-release suffix such as `0.1.0-alpha.1`, released as a pre-release. Derive future tag `desktop-v<version>`; stop
   if the configured version is missing or is not a semantic version.
2. Find the last desktop release: the highest `desktop-v*` tag (`git tag --list 'desktop-v*' --sort=-v:refname`).
   Inspect `git log <tag>..HEAD` and `git diff <tag>...HEAD`, and trace representative runtime and presentation paths
   for outcomes desktop users notice: the `desktop` module, desktop source sets, and shared code as the desktop app
   runs it. Changes to `app/` and Android source sets are not the desktop app's.
3. With no `desktop-v*` tag yet, this is the desktop app's first release: there is no earlier version to compare
   with, so describe what the app offers at `HEAD` in a few `✨ Added` outcomes instead of listing changes, and ask the
   user to confirm them before writing.

## Update desktop/CHANGELOG.md

1. Update `desktop/CHANGELOG.md` for the configured version using the shared writing rules. Keep its own
   `[Unreleased]` link, comparing the target `desktop-v<version>` tag with `HEAD`, and add or update the target
   version's link to its release, `https://github.com/pa2x2/katari/releases/tag/desktop-v<version>`.
2. Leave Mihon out: the desktop app has no Mihon version it is based on.
