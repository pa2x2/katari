---
name: release-notes
description: Generate Katari release notes for the Android app, the desktop app and the extension SDK. Use when asked to run or prepare a Katari release changelog, for either app or both.
---

The Android and desktop apps are versioned and released apart, from the same `main`: the Android app from `versionName`
in `app/build.gradle.kts` and `CHANGELOG.md`, tagged `v<version>`, and the desktop app from `version` in
`desktop/build.gradle.kts` and `DESKTOP-CHANGELOG.md`, tagged `desktop-v<version>`. A push to `main` releases each app
whose version has notes and no tag yet, so one release can carry either app or both. This skill decides which apps the
release covers, holds the rules every changelog follows and the SDK's changelog, and hands each app's changelog to its
own skill.

## Decide what the release covers

1. Read both apps' configured versions and both changelogs. An app is in the release when the user names it, or, when
   they name none, when its configured version has no section in its changelog yet and its last tag doesn't hold it.
   Ask when it stays unclear which apps the user means.
2. For each app in the release, follow its skill: `.agents/skills/android-release-notes/SKILL.md` and
   `.agents/skills/desktop-release-notes/SKILL.md`. Each finds its own range of changes, since the last release of the
   other app may have moved `main` without releasing this one.
3. Review SDK changes as described below, whichever apps the release covers.

## Changelog writing rules

1. Verify final behavior in the relevant source and tests before claiming an outcome.
2. Build a shortlist of release-note candidates by outcome for the file's audience, not by commit.
   Combine related commits into one outcome and discard duplicate, superseded, reverted,
   or implementation-only work. A large commit range may legitimately produce only a few
   bullets.
   Pick each outcome's category heading from its final behavior. Omit follow-up fixes that
   merely complete, correct, or safeguard the expected behavior of a feature introduced in
   the same release range; they are not separate release-note outcomes.
3. SDK changelog must be more detailed in technical changes as it is aimed for developers.
4. Omit by default:
   - documentation, comments, translations, formatting, lint, and typo-only changes;
   - test additions, test fixes, fixtures, snapshots, and test infrastructure;
   - refactors, renames, code cleanup, dependency updates, build/CI/release plumbing,
     telemetry plumbing, and developer tooling;
   - internal APIs, database migrations, and implementation details with no verified effect on the audience;
   - intermediate fixes whose final released behavior is unchanged, and fixes for bugs
     introduced and corrected entirely within the same release range;
   - inherited Mihon changes, unless Katari adapts them in a way the audience needs to know.

5. Use a Keep a Changelog-compatible section named `[X.Y.Z]` with the current date for a
   numbered release, or an undated `[Unreleased]` section for pending changes. Use only the
   applicable decorated category headings from this mapping:

   - `✨ Added` - for new features.
   - `🔄 Changed` - for changes in existing functionality.
   - `🧩 Improved` - for enhancement in existing functionality.
   - `🗑️ Removed` - for now removed features.
   - `🐛 Fixed` - for any bug fixes.
   - `🧩 Other` - for technical stuff.
   - `⚡️ Performance` - for optimizations in existing functionality.

   Use this shape:

   ```markdown
   ## [X.Y.Z] - YYYY-MM-DD

   ### ✨ Added

   - A distinct outcome for the changelog's audience.
   ```

6. Use `unslop` skill to write notes. If it is unavailable, describe outcomes in concise,
   natural, polished language. Let each heading
   provide the category context. Keep the tone factual rather than promotional, and make
   each bullet understandable without commit or implementation context.
7. Preserve released entries and avoid repeating documented outcomes. Keep `[Unreleased]`
   first and numbered releases in descending version order. When preparing a release,
   move its applicable unreleased notes into the release section. If there are no new
   outcomes for a file, leave it unchanged and report that it was reviewed; do not create
   empty sections.
8. An app's changelog takes only what users of that app notice. Code both apps share reaches each changelog its users
   notice it in, checked against that app's own code path; a change to one app's own code never appears in the other's.

## Update SDK-CHANGELOG.md

1. Inspect SDK changes only under the modules published by `jitpack.yml`, since the last SDK tag (`sdk-*`).
   - `entry-source-api/`: extension source contracts, capabilities, filters, and media models.
   - `book-api/`: shared BOOK content, publication, and document contracts.
   - `core/common/`: the published supporting library; consider only changes affecting
     extension consumers.
2. New SDK must already be tagged and released. Use its tag to determine a version.
3. Update `SDK-CHANGELOG.md` for that SDK version.
4. When Katari stops loading extensions built with the previous SDK, or extensions built with this one can't load in
   the previous Katari, the release needs an upgrade guide on the website. Hand that to the `website-update` skill, and
   put one quoted line under the version's heading that links the guide by its full address on the site:

   > From SDK 2.x, [Upgrade to SDK 3.0](https://pa2x2.github.io/katari/developers/upgrade/3.0/) walks through the changes.

## Update the website

After the changelogs are written, run the `website-update` skill and give it the outcomes added to each changelog
as the list to check. Report its result together with the changelog changes.

## Guidance

- Derive release-note content exclusively from the checked-out branch's changes since each changelog's last release
  tag. The configured versions' tags are future release identifiers for changelog links, not comparison endpoints.
- Create a temporary file to track your findings and take notes during exploration.
