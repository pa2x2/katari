---
name: release-notes
description: Generate user-facing Katari release notes for the configured app version by comparing the current branch against main, then update CHANGELOG.md. Use when asked to run or implement the release-notes command or prepare a Katari release changelog.
---

# Katari release notes

Prepare release notes from verified repository changes, not commit subjects alone. Keep
Katari's changelog focused on behavior that differs from Mihon.

## Inspect the release

1. Read `versionName` from `app/build.gradle.kts` as the release version. Require a stable
   semantic version such as `1.1.0` and derive future tag `v1.1.0`; stop if the configured
   version is missing or is not a stable semantic version. Do not require a version argument.
2. Read `AGENTS.md`. Verify the repository root and worktree state. Preserve unrelated
   worktree changes.
3. Compare the checked-out branch with the local `main` branch. Do not fetch, push, change
   branches, require GitHub authentication, or inspect a GitHub release. Run:

   ```bash
   git branch --show-current
   git rev-parse HEAD
   git rev-parse --verify 'main^{commit}'
   git merge-base main HEAD
   ```

   Record the current branch, `HEAD` SHA, `main` SHA, merge base, comparison range
   `main...HEAD`, and configured app version. Stop if `HEAD` is detached, local `main` is
   unavailable, or the branch has no changes relative to `main`. Do not require the future
   release tag to exist.
4. Inspect `git log main..HEAD`, `git diff --stat main...HEAD`, and the full
   `git diff main...HEAD`. Use `CHANGELOG.md`, pull-request references, tests, and commit
   messages as leads. Trace
   representative runtime and presentation paths before claiming user-visible behavior.
   When local history identifies an associated pull request, record its number and URL for
   the corresponding outcome. Read the public pull-request metadata to verify the PR author's
   GitHub login; this is read-only and must not change the pull request or release. Do not
   guess a PR association or contributor login.
5. Build a shortlist of release-note candidates by user-visible outcome, not by commit.
   Combine related commits into one outcome and discard duplicate, superseded, reverted,
   or implementation-only work. A large commit range may legitimately produce only a few
   bullets.
   Classify each outcome by its final behavior rather than the commit subject. Omit
   follow-up fixes that merely complete, correct, or safeguard the expected behavior of a
   feature introduced in the same release range; they are not separate release-note
   outcomes. If such a change creates a materially distinct user-facing outcome, combine
   it with that feature under `Added`, `Changed`, or `Improved`, never under `Fixed`.
   Reserve `Fixed` for independently user-visible bugs or regressions outside a new
   feature's expected behavior.
6. Add contributor credit only for outcomes associated with pull requests. Use each PR
   author's GitHub login from the verified PR metadata; do not infer a login from a commit
   author name, email, or branch name. When one outcome combines several pull requests,
   preserve the union of their PR authors. Do not credit merge, dependency, or automation
   bots. If an associated PR's author login cannot be verified, stop and ask the user rather
   than inventing a mention. Outcomes without an associated PR receive no contributor
   credit, even when their commit authors are known.
7. Keep a candidate only when the verified final behavior gives users something useful to
   know: a feature they can use, a meaningful behavior or workflow change, a user-facing
   fix, a compatibility change that requires action, or a removal they may notice. If a
   regular user would not benefit from knowing it when deciding to update or using the new
   version, omit it.
8. Omit by default:

   - documentation, comments, translations, formatting, lint, and typo-only changes;
   - test additions, test fixes, fixtures, snapshots, and test infrastructure;
   - refactors, renames, code cleanup, dependency updates, build/CI/release plumbing,
     telemetry plumbing, and developer tooling;
   - internal APIs, database migrations, performance claims, and implementation details
     with no verified user-visible effect;
   - intermediate fixes whose final released behavior is unchanged, and fixes for bugs
     introduced and corrected entirely within the same release range;
   - inherited Mihon changes, unless Katari adapts them in a way Katari users need to know.

   Include an otherwise omitted item only when its concrete impact is material to users or
   extension developers. Describe that impact, never the maintenance work itself.

## Update the changelog

1. Write a polished, Keep a Changelog-compatible section named `[X.Y.Z]` with the current
   date. For major and minor release versions follow the heading with a `🌟 Highlights`
   subsection containing one concise sentence that summarizes the release's most meaningful
   verified user benefit. Write the sentence as normal paragraph text, not a blockquote, list item,
   or GitHub alert. Make the highlight specific to the release; Use only the applicable decorated category
   headings from this mapping:

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

   ### 🌟 Highlights

   A concise, user-focused highlight of the release.

   ### ✨ Added

   - A distinct user-visible outcome.
   ```

2. Describe outcomes in concise, natural, polished language. Let each heading
   provide the category context. Keep the tone factual rather than promotional, and make
   each bullet understandable without commit or implementation context.
3. Update `CHANGELOG.md` in the worktree. Insert the complete release section in descending
   version order without changing existing release text. Update `[Unreleased]` to compare
   the target tag with `HEAD`, and add or update the target version's release link using the
   repository URL established by the existing link definitions. Preserve unrelated
   worktree changes and do not modify any other repository file.
4. If changes include merge with upstream repository of Mihon - include information
   on the version which was merged in the following format:

   ## Based on [Mihon <version>](<link to mihon release on github>)

## Safety rules

- Treat `/release-notes` as a worktree-only changelog update. Do not modify files other than
  `CHANGELOG.md`.
- Derive release-note content exclusively from the checked-out branch's changes relative to
  local `main`. The configured app version's `vX.Y.Z` tag is a future release identifier for
  changelog links, not a required comparison endpoint.
