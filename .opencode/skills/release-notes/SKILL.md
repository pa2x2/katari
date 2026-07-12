---
name: release-notes
description: Generate user-facing Katari release notes for a specified semantic version, update CHANGELOG.md from the previous Katari release tag, and update the matching GitHub draft release after explicit confirmation. Use when asked to run or implement the release-notes command, prepare a Katari release changelog, or populate a draft GitHub release.
---

# Katari release notes

Prepare release notes from verified repository changes, not commit subjects alone. Keep
Katari's changelog focused on behavior that differs from Mihon.

## Inspect the release

1. Require exactly one stable semantic version. Normalize `1.1.0` to `v1.1.0`; reject
   prereleases, ranges, branches, and arbitrary commits.
2. Read `AGENTS.md`. Verify the repository root, worktree state, `origin` URL, and GitHub
   authentication. Preserve unrelated worktree changes.
3. Fetch tags from `origin` without pushing or changing branches. Run:

   ```bash
   python3 .opencode/skills/release-notes/scripts/inspect_release.py <version>
   ```

   Record the target tag, target SHA, previous Katari release tag, comparison range,
   configured app version, and GitHub release state. Stop if the target tag does not
   exist, is not descended from the previous tag, does not match the app version, or the
   GitHub release is missing or is not a draft.
4. Inspect `git log`, `git diff --stat`, and the full diff for the reported range. Use
   `CHANGELOG.md`, pull-request references, tests, and commit messages as leads. Trace
   representative runtime and presentation paths before claiming user-visible behavior.
5. Build a shortlist of release-note candidates by user-visible outcome, not by commit.
   Combine related commits into one outcome and discard duplicate, superseded, reverted,
   or implementation-only work. A large commit range may legitimately produce only a few
   bullets.
6. Keep a candidate only when the verified final behavior gives users something useful to
   know: a feature they can use, a meaningful behavior or workflow change, a user-facing
   fix, a compatibility change that requires action, or a removal they may notice. If a
   regular user would not benefit from knowing it when deciding to update or using the new
   version, omit it.
7. Omit by default:

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

## Update CHANGELOG.md

1. Write a Keep a Changelog section named `[X.Y.Z]` with the current date. Use only the
   applicable headings `Added`, `Changed`, `Improved`, `Removed`, `Fixed`, and `Other`.
2. Describe user-visible outcomes in concise past-tense-neutral bullets. Mention extension
   developer changes only when they alter Katari's public SDK or compatibility contract.
   Do not create one bullet per commit or pull request, include commit hashes or PR numbers,
   or add a catch-all list of minor/internal changes. Prefer fewer, broader bullets that each
   communicate one distinct and useful outcome.
3. Insert the new section above the newest released version. If an `Unreleased` section
   exists, move only entries verified for this release and leave the heading in place.
4. Update link definitions so `[Unreleased]` compares `vX.Y.Z...HEAD` and `[X.Y.Z]` links
   to the matching Katari release. Preserve prior definitions.
5. Apply the changelog edit without asking for confirmation. Run formatting-neutral checks
   such as `git diff --check` and inspect the resulting diff. Do not stage or commit it.

## Prepare the GitHub draft

1. Create a shorter release body from the same verified facts. Prefer sections such as
   `What's new`, `Improvements`, and `Fixes`, omitting empty sections. Keep wording useful
   to regular users and avoid duplicating every changelog detail. Select only the highlights
   most likely to affect use of the app; do not pad the body to represent every commit or
   every changelog bullet.
2. End with this download guidance, using the normalized tag:

   ```markdown
   > [!TIP]
   >
   > If you are unsure which version to download, use `katari-vX.Y.Z.apk`.
   ```

3. Show the complete proposed body and summarize the local `CHANGELOG.md` change. Ask the
   user for explicit confirmation to update the GitHub draft. Updating the changelog does
   not imply this confirmation.
4. Do not call `gh release edit` in the same turn that asks for confirmation. Continue only
   after a later user message clearly approves the displayed body.
5. Immediately before updating, re-read the release with:

   ```bash
   gh release view <tag> --json tagName,name,isDraft,isPrerelease,body,url
   ```

   Stop if it is missing, no longer a draft, or its tag differs. If its body changed since
   the preview, show the change and ask again instead of overwriting it.
6. Write the approved body through a temporary file and run `gh release edit <tag>
   --notes-file <file>`. Always remove the temporary file. Never change draft/prerelease
   state, title, tag, target, or assets.
7. Read the release back, verify the body, and report its URL plus the uncommitted changelog
   path. Never publish, commit, stage, tag, or push.

## Safety rules

- Treat `/release-notes <version>` as authorization to edit only `CHANGELOG.md`; require
  separate confirmation for the external GitHub write.
- Do not overwrite unrelated changelog edits. If the intended insertion overlaps existing
  user changes and cannot be preserved confidently, stop and explain the conflict.
- Never infer a release from `HEAD`; resolve both endpoints as exact `vX.Y.Z` tags.
