---
name: release-notes
description: Generate Katari app and extension SDK release notes. Use when asked to run or prepare a Katari release changelog.
---

## Inspect the release

1. Read `versionName` from `app/build.gradle.kts` as the release version. Require a stable
   semantic version such as `1.1.0` and derive future tag `v1.1.0`; stop if the configured
   version is missing or is not a stable semantic version. If the app version is already
   covered, skip its release section but still review SDK changes.
2. Read both changelogs and compare the checked-out branch with local `main`, using the
   inspection scope in each file's update section below.

## Changelog writing rules

1. Verify final behavior in the relevant source and tests before claiming an outcome.
2. Build a shortlist of release-note candidates by outcome for the file's audience, not by commit.
   Combine related commits into one outcome and discard duplicate, superseded, reverted,
   or implementation-only work. A large commit range may legitimately produce only a few
   bullets.
   Classify each outcome by its final behavior rather than the commit subject. Omit
   follow-up fixes that merely complete, correct, or safeguard the expected behavior of a
   feature introduced in the same release range; they are not separate release-note outcomes.
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

5. Use `unslop` skill to write notes. If it is unavailable, describe outcomes in concise,
   natural, polished language. Let each heading
   provide the category context. Keep the tone factual rather than promotional, and make
   each bullet understandable without commit or implementation context.
7. Preserve released entries and avoid repeating documented outcomes. Keep `[Unreleased]`
   first and numbered releases in descending version order. When preparing a release,
   move its applicable unreleased notes into the release section. If there are no new
   outcomes for a file, leave it unchanged and report that it was reviewed; do not create
   empty sections.

## Update CHANGELOG.md

1. Inspect `git log main..HEAD` and `git diff main...HEAD`. Trace representative runtime
   and presentation paths for app-facing outcomes.
2. Update `CHANGELOG.md` for the configured app version using the shared writing rules.
   Update `[Unreleased]` to compare the target tag with `HEAD`, and add or update the target
   version's release link using the repository URL established by the existing link definitions.
3. If changes include merge with upstream repository of Mihon - include information
   on the version which was merged in the following format:

   ## Based on [Mihon <version>](<link to mihon release on github>)

## Update SDK-CHANGELOG.md

1. Inspect SDK changes only under the modules published by `jitpack.yml`.
   - `entry-source-api/`: extension source contracts, capabilities, filters, and media models.
   - `book-api/`: shared BOOK content, publication, and document contracts.
   - `core/common/`: the published supporting library; consider only changes affecting
     extension consumers.
2. New SDK must already be tagged and released. Use its tag to determine a version.
3. Update `SDK-CHANGELOG.md` for that SDK version.

## Guidance

- Derive release-note content exclusively from the checked-out branch's changes relative to
  local `main`. The configured app version's `vX.Y.Z` tag is a future release identifier for
  changelog links, not a required comparison endpoint.
- Create a temporary file to track your findings and take notes during exploration.
