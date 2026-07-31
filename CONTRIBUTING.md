# Contributing to Katari

This guide covers repository-specific work on Katari and the Entry SDK.

## Commit messages

Commit subjects must use the following format:

```text
(type): summary
```

Activate the repository's commit-message hook once in each clone:

```bash
git config core.hooksPath .githooks
```

Use one of these types:

- `feat`: add or extend functionality.
- `fix`: correct faulty behavior.
- `docs`: change documentation only.
- `style`: change formatting without affecting behavior.
- `refactor`: restructure code without changing behavior.
- `perf`: improve performance.
- `test`: add or update tests.
- `build`: change the build system or dependencies.
- `ci`: change continuous-integration configuration.
- `chore`: perform maintenance not covered by another type.
- `revert`: revert an earlier change.

For example:

```text
(feat): add profile export
(fix): preserve prose reader position
(docs): explain SDK versioning
```

Git-generated subjects beginning with `Merge `, `Revert `, `fixup! `, or `squash! ` are exempt from this format. Commit-message bodies are unrestricted.

## Inspect feature relationships

When changing an application or content-type provider, Feature integration, contextual rule, contract, or specialized
adapter,
generate the evaluated developer report from the repository root:

```bash
./gradlew generateFeatureReport
```

The task writes `entry-interactions/build/reports/features/developer-report.txt`. The report separates application
Features from Entry content-type Features and lists discovered providers, every evaluated integration, conditional
inputs and possible blockers, selected or conditional consequences, contract results, projections, and obligations
with their responsible owners. A passing contextual validation scenario remains labeled as a scenario and does not
establish subject-wide support.

## Change the public Entry SDK

For a public API change:

1. Classify it as patch, minor, or major.
2. Add KDoc for every new or changed public declaration and member.
3. Add focused SDK tests and app-side tests for host recognition and fallback behavior.
4. Publish `local-SNAPSHOT` and compile a representative extension against it.
5. Test the extension with the matching Katari runtime rather than only compiling it.
6. Record the change and the first supporting Katari version in the changelog.

When adding a content type, define its user-facing meaning, child-item semantics, media contract, applicable capabilities, runtime behavior, compatibility level, and minimum Katari version.

An SDK artifact and its runtime support must be released coherently. Do not publish a stable SDK tag for a symbol absent from the corresponding Katari runtime, and do not present an unreleased symbol as available in an older stable artifact.
