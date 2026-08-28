# Repo Notes

## Layout
- `app/` is the runtime app. Shared code lives in `core/*`, `data`, `domain`, `presentation-*`, `source-*`, `i18n`, `telemetry`, and `macrobenchmark`.
- Custom Gradle plugins and tasks live in `gradle/build-logic/`. `settings.gradle.kts` enables type-safe project accessors and rejects project-level repositories, so add repos only there and use catalog/accessor entries instead of hardcoded versions or string project paths.

## Source organization
- A source directory must represent one cohesive responsibility. Group files by the feature, bounded context, or runtime layer that owns them; do not accumulate unrelated feature contracts, implementations, providers, and helpers in a module-root directory.
- A handwritten source or test file must have one independently nameable responsibility. A shared feature or entry type is not, by itself, a sufficiently narrow file owner.
- Keep activities and top-level UI surfaces focused on lifecycle and composition. Move independently testable navigation, session coordination, rendering modes, settings panels, parsing, and platform adaptation into ownership-named files.
- Keep parser traversal and assembly separate from semantic block construction, inline rendering, style decoding, and format-specific helpers when those concerns can change independently.
- Divide test files by durable behavior boundary. Put reused fixtures in explicitly named, owner-specific fixture files rather than allowing a test class to accumulate unrelated scenarios and setup.
- File splits must be semantic. Do not create numbered `Part1`/`Part2` files, distribute private helpers arbitrarily, or use vague containers such as `common`, `misc`, or `utils` to make a file appear smaller.
- When a change introduces or exposes a second responsibility in a file, extract that responsibility in the same change instead of postponing the cleanup.
- Keep only genuine module-wide entry points and composition roots at the source root. When a module contains multiple responsibilities, create clearly named subdirectories for them as part of the same change that introduces or exposes the split.
- Mirror the production directory structure in tests so behavior and its coverage remain discoverable together.
- Avoid catch-all directories such as `common`, `misc`, or `utils`. Name structural groups after concrete ownership, and place narrowly shared helpers with the feature that owns their semantics.
- Before finishing a change, inspect every touched source file and directory. If responsibilities or ownership are not clear from the tree alone, reorganize that area before committing rather than leaving cleanup for a follow-up.

## Test quality
- Every test must protect a distinct, durable behavior, compatibility guarantee, failure mode, or architectural invariant. Do not retain tests whose only purpose was to observe an implementation while it was being developed.
- Before adding a test, inspect existing unit, integration, regression, and contract coverage for the same behavior. Do not repeat the same assertion at the same boundary; overlapping tests must each protect a distinct regression or integration seam.
- Test observable behavior through the narrowest stable boundary that owns it. Avoid assertions about internal intermediate state, private control flow, or paths that supported production callers cannot reach.
- Avoid tests for pass-through wrappers or exact collaborator call sequences unless the wrapper adds meaningful branching, transformation, ordering, transaction, or error semantics.
- A test must be capable of failing for a plausible regression. Avoid tautological assertions, duplicating production calculations in the test, or snapshots of incidental structure with no reviewed compatibility requirement.
- Do not promote incidental representations, historical implementation details, or arbitrary mock outputs into product contracts. Assertions about exact presentation, persistence, sequencing, or compatibility policy must be traceable to an established requirement, supported prior behavior, or an authoritative producer contract.
- Mocks and fakes must preserve the real collaborator's documented guarantees and supported output space. Do not manufacture inputs that production cannot emit merely to exercise consumer-side normalization; test that invariant at the boundary that actually owns it.
- Before finishing a change, re-read every added or modified test and remove or narrow cases that are now subsumed by stronger coverage introduced later in the same change.

## Toolchain
- Android SDK/NDK and Java compatibility come from `gradle/mihon.versions.toml` plus build logic; do not hardcode them per module.
- Do not use `--quiet` for compilation, tests, lint, or assembly because it suppresses warnings. Run Gradle with `--console=plain --warning-mode=all`, redirect the complete output to a unique file under `/tmp`, preserve the Gradle exit code, and inspect only diagnostic matches and nearby context (`w:`, `warning`, `e:`, `error`, `FAILURE`, `What went wrong`, and `Caused by`). If the build fails, inspect additional portions of the saved log selectively instead of printing the complete log. Use `--quiet` only for tasks whose diagnostic output is irrelevant.

## Validation
- Run `./gradlew spotlessApply` from the repo root.
- App unit tests run on the `foss` buildType (`testBuildType = "foss"`); focused example: `./gradlew :app:testFossUnitTest --tests '...'`.
- FOSS compilation can be verified with `:app:compileFossKotlin`; telemetry-enabled release compilation uses `:app:compileReleaseKotlin -Pinclude-telemetry`.
- Never combine FOSS/unit/architecture tasks with `-Pinclude-telemetry` or `-Penable-updater` in the same Gradle invocation. Those project properties affect every configured variant and can make `processFossGoogleServices` reject the `app.katari.foss` application ID. Run FOSS checks without telemetry/updater properties, let that invocation finish, then run telemetry-enabled release compilation or assembly in a separate invocation.
- Do not infer the installable variant for emulator/device validation from the `foss` unit-test buildType. Before installing, identify the package that is actually running and use the matching Gradle variant: `installDebug` installs `app.katari.dev`, while `installFoss` installs the separate `app.katari.foss` application. After installation, verify that the intended package was launched and that its process changed or restarted; installing a different application ID does not update the app under test.
- After touching `data/src/main/sqldelight`, run `./gradlew verifySqlDelightMigration`.

## Guidance
- When asked to fix the issue - never simply apply the easiest fix without finding the reason of the issue. Band-aid solutions are not welcomed. The goal is to fix the reason issue arised in the first place, not to merely fix the symptom
- Introduced warnings must not be left un-addressed. Not just suppressed so that thwy no longer show up, but cause of their appearance should be fixed instead

## Commit classification

- Before committing, inspect the complete staged diff and classify the commit by its primary purpose.
- Use `fix` when restoring behavior that was faulty relative to existing expectations.
- Use `perf` when improving performance without introducing a new capability.
- Use `feat` when adding or extending a user-facing or developer-facing capability.
- Use `refactor` when restructuring production code without changing observable behavior.
- Use `docs`, `test`, `build`, or `ci` only when that concern is the commit's primary purpose.
- Use `style` only for source formatting without behavioral changes; never use it for visual or UI changes.
- Use `deps` when adding, removing, or updating external dependencies is the commit's primary purpose.
- Use `chore` for maintenance that does not fit a more specific type; it is the fallback, not the default.
- Use `revert` when reversing an earlier commit.
- Supporting tests, documentation, formatting, or refactoring do not determine the type when the commit primarily fixes or adds behavior. A bug fix with regression tests is still `fix`.
- If independently meaningful changes require different types, split them into separate commits. If classification remains ambiguous, present the proposed subject and rationale to the user before committing.
- Use exactly `(type): summary` and never bypass the commit-message hook.
