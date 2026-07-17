---
name: release-notes
description: Generate user-facing Katari release notes for a specified local semantic-version tag by comparing it with its previous local Katari release tag, then update CHANGELOG.md. Use when asked to run or implement the release-notes command or prepare a Katari release changelog.
---

# Katari release notes

Prepare release notes from verified repository changes, not commit subjects alone. Keep
Katari's changelog focused on behavior that differs from Mihon.

## Inspect the release

1. Require exactly one stable semantic version. Normalize `1.1.0` to `v1.1.0`; reject
   prereleases, ranges, branches, and arbitrary commits.
2. Read `AGENTS.md`. Verify the repository root and worktree state. Preserve unrelated
   worktree changes.
3. Use local tags only. Do not fetch, push, change branches, require GitHub authentication,
   or inspect a GitHub release. Run:

   ```bash
   python3 .opencode/skills/release-notes/scripts/inspect_release.py <version>
   ```

   Record the target tag, target SHA, previous local Katari release tag, comparison range,
   and configured app version. Stop if the target tag does not exist locally, is not
   descended from the previous local tag, or does not match the app version.
4. Inspect `git log`, `git diff --stat`, and the full diff for the reported range. Use
   `CHANGELOG.md`, pull-request references, tests, and commit messages as leads. Trace
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

1. Write a Keep a Changelog section named `[X.Y.Z]` with the current date. Use only the
   applicable headings `Added`, `Changed`, `Improved`, `Removed`, `Fixed`, and `Other`.
2. Describe user-visible outcomes in concise past-tense-neutral bullets. Mention extension
   developer changes only when they alter Katari's public SDK or compatibility contract.
   Do not create one bullet per commit or pull request, include commit hashes, or add a
   catch-all list of minor/internal changes. Prefer fewer, broader bullets that each
   communicate one distinct and useful outcome. For an outcome with an associated pull
   request, end its bullet with the verified PR-author credit in the form `(by @user)` or
   `(by @user1, @user2)`, immediately followed in the same paragraph by the PR Markdown link
   in the form `([#123](https://github.com/OWNER/REPO/pull/123))`. Never place a pull-request
   link on a standalone continuation line. Append every associated PR link when one outcome
   combines multiple pull requests. For an outcome without an associated PR, omit both the
   contributor credit and PR link; never use a commit-author display name as a substitute.
3. Update `CHANGELOG.md` in the worktree. Insert the complete release section in descending
   version order without changing existing release text. Update `[Unreleased]` to compare
   the target tag with `HEAD`, and add or update the target version's release link using the
   repository URL established by the existing link definitions. Preserve unrelated
   worktree changes and do not modify any other repository file.
4. Return the complete section in a Markdown code block in the response. When invoked by a
   command that requests a GitHub release body, show that exact proposed body and require
   explicit user confirmation before editing the GitHub release. Do not stage or commit
   changes, create or push tags, or create or publish a GitHub release.

## Safety rules

- Treat `/release-notes <version>` as a worktree-only changelog update. Do not modify files
  other than `CHANGELOG.md`.
- Never infer a release from `HEAD`; resolve both endpoints as exact `vX.Y.Z` tags.
