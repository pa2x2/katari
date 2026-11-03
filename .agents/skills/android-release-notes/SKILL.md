---
name: android-release-notes
description: Write the Android app's release notes in CHANGELOG.md. Run through the release-notes skill, which decides what a Katari release covers and holds the writing rules.
---

Follow the release-notes skill (`.agents/skills/release-notes/SKILL.md`) for what the release covers and for its
changelog writing rules; this skill adds what only the Android app's changelog needs.

## Inspect the release

1. Read `versionName` from `app/build.gradle.kts` as the release version. Require a stable
   semantic version such as `1.1.0` and derive future tag `v1.1.0`; stop if the configured
   version is missing or is not a stable semantic version.
2. Find the last Android release: the highest `v*` tag (`git tag --list 'v*' --sort=-v:refname`), which holds no
   desktop or SDK release. Inspect `git log <tag>..HEAD` and `git diff <tag>...HEAD`, and trace representative runtime
   and presentation paths for outcomes Android users notice: the `app` module, Android source sets, and shared code
   as the Android app runs it. Changes to `desktop/` and desktop source sets are not the Android app's.

## Update CHANGELOG.md

1. Update `CHANGELOG.md` for the configured version using the shared writing rules.
   Update `[Unreleased]` to compare the target tag with `HEAD`, and add or update the target
   version's release link using the repository URL established by the existing link definitions.
2. If changes include merge with upstream repository of Mihon - include information
   on the version which was merged in the following format:

   ## Based on [Mihon <version>](<link to mihon release on github>)
