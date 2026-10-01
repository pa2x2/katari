# Repo Notes

## Layout
- `app/` is the runtime app. Shared code lives in `core/*`, `data`, `domain`, `presentation-*`, `source-*`, `i18n`, `telemetry`, and `macrobenchmark`.
- Custom Gradle plugins and tasks live in `gradle/build-logic/`. `settings.gradle.kts` enables type-safe project accessors and rejects project-level repositories, so add repos only there and use catalog/accessor entries instead of hardcoded versions or string project paths.

## Source organization
- A source directory must represent one cohesive responsibility. Group files by the feature, bounded context, or runtime layer that owns them; do not accumulate unrelated feature contracts, implementations, providers, and helpers in a module-root directory.
- A handwritten source or test file must have one independently nameable responsibility. A shared feature or entry type is not, by itself, a sufficiently narrow file owner.
- Keep activities and top-level UI surfaces focused on lifecycle and composition. Move independently testable navigation, session coordination, rendering modes, settings panels, parsing, and platform adaptation into ownership-named files.
- Keep parser traversal and assembly separate from semantic block construction, inline rendering, style decoding, and format-specific helpers when those concerns can change independently.
- Divide test files by durable behavior boundary rather than letting one test class accumulate unrelated scenarios and setup.
- File splits must be semantic. Do not create numbered `Part1`/`Part2` files, distribute private helpers arbitrarily, or use vague containers such as `common`, `misc`, or `utils` to make a file appear smaller.
- When a change introduces or exposes a second responsibility in a file, extract that responsibility in the same change instead of postponing the cleanup.
- Keep only genuine module-wide entry points and composition roots at the source root. When a module contains multiple responsibilities, create clearly named subdirectories for them as part of the same change that introduces or exposes the split.
- Mirror the production directory structure in tests so behavior and its coverage remain discoverable together.
- Avoid catch-all directories such as `common`, `misc`, or `utils`. Name structural groups after concrete ownership, and place narrowly shared helpers with the feature that owns their semantics.
- Before finishing a change, inspect every touched source file and directory. If responsibilities or ownership are not clear from the tree alone, reorganize that area before committing rather than leaving cleanup for a follow-up.

## Comments
A comment earns its place by telling the reader something the code can't: why it's done this way, a constraint or platform quirk it works around, a bug it prevents, or what a non-obvious value means. If deleting the comment loses nothing a careful reader couldn't get from the code, delete it.

- **Don't restate the code.** No `/** Cancels a running download. */` over `cancelDownload`, no `// Refresh on resume` over an `onResume` override, no `/** Last failure, as a user-facing message. */` over `error: String?`.
- **Don't narrate names or types.** If a property, parameter or function name plus its type already says it, leave it bare. Document a property only when its meaning isn't obvious: units, what `null` stands for, who sets it, what it must never be.
- **Don't write file headers that only name the file.** "Tests for the library repository" or "Chapter list screen model" add nothing. A header is worth it when it explains a design: how the parts fit, what the module owns, a contract callers rely on.
- **Keep comments true.** When you change code, update or delete the comments it touches. A stale comment is worse than none.

## Writing tests
A change does not come with tests by default. Most features and fixes need none: Kotlin compilation (warnings are errors), spotless, the architecture boundary checks, and trying the change on the test device catch most mistakes. Tests written just because a change was made are how this suite once grew to more than two thirds the size of the production code, and most of them were deleted.

Tests live in the module's `src/test` (or `androidTest` when they need a real device), mirroring the production package.

### When to write one
Write a test only when all three hold:

1. **The bug would go unnoticed.** Compilation, the boundary checks, and using the feature once on the device wouldn't catch it. Think races and cancellation, what survives process death or an app restart, database migrations and backup restore, profile isolation, parsers fed real-world documents, source and extension compatibility, and error paths you can't easily trigger by hand.
2. **The bug is likely.** Either it has happened before, or the logic is subtle enough that a reasonable edit would break it. "What if someone deletes this line" doesn't count.
3. **Nothing else catches it.** No existing test, type, boundary check, or feature contract verifier already covers it.

Name the bug before you write the test. "Checks that library filtering works" is not a bug. "Filtering one profile's library hides entries in another profile" is. If you can only describe what the code does, not how it would go wrong, don't write the test.

### How to write one
- **One test per bug, not per branch.** Don't list every entry type, enum value, status or error class. Cover only the cases that differ from the obvious.
- **Assert related things together.** One test can check that a failed refresh keeps the cached chapters *and* reports the error. Don't split those into separate tests with the same setup.
- **Test where the logic lives.** Call the function, interactor, or state holder that owns it, not a screen model, activity, or composable that uses it.
- **No new test infrastructure.** If a test needs a new fixture file, fake repository, test composition, or Robolectric/Compose harness, it is probably the wrong test. Move the logic into a plain function and test that. Prefer real objects and existing fakes such as `InMemoryPreferenceStore` over mocks.
- **Fakes keep the real guarantees.** A mock or fake must only produce what the real collaborator can. Don't manufacture impossible inputs to exercise a consumer's normalization; test that invariant where it is owned.
- **Re-read before finishing.** Delete any test the rest of the change made redundant.

### Don't write these
Every one of these has been written here before and deleted.

- **Library and platform behaviour.** Don't test that kotlinx.serialization round-trips a class, that a `StateFlow` holds what was emitted, that a Compose `onClick` fires, or that a SQLDelight query does what its SQL says. Those libraries have their own tests.
- **Setter round-trips.** `preference.set(true)` followed by `preference.get() shouldBe true`, or `screenModel.setX(v)` followed by `state.x shouldBe v`, only restates the setter. Test the logic instead: ordering, dedupe, merging, error handling, what persists.
- **Constants and static config.** Asserting default preference values, a feature descriptor's fields, contract or capability ids, enum entries, or a DI module's bindings just copies the source. Test the behaviour that uses the value.
- **Tautologies.** If the expected value is computed the same way the implementation computes it, the test can't fail. Compare against an independent source: a literal, a real document or wire payload, or a hand-written expectation.
- **Tests of a mock.** Calling a function and then `coVerify`-ing that it forwarded to a mocked repository only checks the mock. Pass-through interactors, delegating wrappers, and adapters don't need tests. Assert call order only when ordering is itself the behaviour.
- **UI render tests.** Compose and Robolectric tests need harnesses, break on every layout change, and miss what actually goes wrong on a device. If a composable has logic worth testing (enabled rules, filtering, which option is selected), move it into a plain function beside it and test that. Check the UI itself on the device. Device tests are for behaviour that needs real Android text measurement, rendering, or storage and can't be moved to a plain function.
- **Lint rules written as tests.** A test that scans sources, manifests, or the dependency graph for a banned pattern belongs in the build-logic boundary check tasks. Test the checker once per rule, not every path through it.
- **Implementation snapshots.** Tests that pin intermediate state, private control flow, or exact strings and serialized layouts that nothing depends on. Pin a format only when it is a compatibility requirement: backups, database migrations, the extension API, or data read back after an update.
- **Duplicates.** If a mapping is covered by a unit test, one wiring test at the next layer is enough. Don't re-test every branch there, and don't repeat a contract verifier's check in a unit test.

## Toolchain
- Android SDK/NDK and Java compatibility come from `gradle/mihon.versions.toml` plus build logic; do not hardcode them per module.
- Do not use `--quiet` for compilation, tests, lint, or assembly because it suppresses warnings. Run Gradle with `--console=plain --warning-mode=all`, redirect the complete output to a unique file under `/tmp`, preserve the Gradle exit code, and inspect only diagnostic matches and nearby context (`w:`, `warning`, `e:`, `error`, `FAILURE`, `What went wrong`, and `Caused by`). If the build fails, inspect additional portions of the saved log selectively instead of printing the complete log. Use `--quiet` only for tasks whose diagnostic output is irrelevant.

## Validation
- Run `./gradlew spotlessApply` from the repo root.
- App unit tests run on the `foss` buildType (`testBuildType = "foss"`); library modules run theirs on `debug` and multiplatform modules through `allTests`. Focused example: `./gradlew :app:testFossUnitTest --tests '...'`.
- `./gradlew verifyTests --console=plain --warning-mode=all` runs every module's host tests and build-logic tests and compiles every device test. Every change that touches production or test code must pass it; `testFossUnitTest` and other module-specific test tasks cover a single module and leave failures elsewhere unnoticed. Modules join `verifyTests` through the convention plugins, so do not add per-module test tasks to aggregate verification lists.
- Changes to application or Entry Feature runtime modules, production runtime components, platform-service factories used during module installation, the production validation environment, or the Android minimum SDK must pass `./gradlew verifyEntryFeatureArchitecture verifyTests --console=plain --warning-mode=all`. Focused module tests and compilation do not exercise the complete production composition and are insufficient for these changes. Do not report the work complete until this command passes.
- FOSS compilation can be verified with `:app:compileFossKotlin`; telemetry-enabled release compilation uses `:app:compileReleaseKotlin -Pinclude-telemetry`.
- Never combine FOSS/unit/architecture tasks with `-Pinclude-telemetry` or `-Penable-updater` in the same Gradle invocation. Those project properties affect every configured variant and can make `processFossGoogleServices` reject the `app.katari.foss` application ID. Run FOSS checks without telemetry/updater properties, let that invocation finish, then run telemetry-enabled release compilation or assembly in a separate invocation.
- Do not infer the installable variant for emulator/device validation from the `foss` unit-test buildType. Before installing, identify the package that is actually running and use the matching Gradle variant: `installDebug` installs `app.katari.dev`, while `installFoss` installs the separate `app.katari.foss` application. After installation, verify that the intended package was launched and that its process changed or restarted; installing a different application ID does not update the app under test.
- After touching `data/src/main/sqldelight`, run `./gradlew verifySqlDelightMigration`.

## Guidance
- When asked to fix the issue - never simply apply the easiest fix without finding the reason of the issue. Band-aid solutions are not welcomed. The goal is to fix the reason issue arised in the first place, not to merely fix the symptom
- Introduced warnings must not be left un-addressed. Not just suppressed so that thwy no longer show up, but cause of their appearance should be fixed instead
- Kotlin compilation treats warnings as errors (`allWarningsAsErrors` in build-logic). Do not disable it or suppress a warning to get past it; fix what causes the warning.

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
