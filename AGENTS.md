# Repo Notes

## Layout
- `app/` is the Android app, `desktop/` the Linux app, and `app-ui/` the screens both share; only these modules sit at the repository root.
- Every other module lives in the group folder that owns it, and its Gradle path follows the folder (`:core:data`, `:features:translation:runtime`): `core/` holds the foundation the apps build on (data, domain, i18n, icons, presentation-core, telemetry and the shared libraries), `sources/` holds what loads extensions and their content (the extension SDK and compatibility APIs, the extension runtime, the web view), and `features/` holds the apps' features. Put a new module in the group that owns it rather than at the root.
- Developer tools run by hand (builders of committed artifacts, release and publish scripts, probes, harnesses) live in `tools/`, one directory per tool named after its job, together with the baseline profile generator and the showcase pipeline. Modules keep only what their own build runs; never add such scripts inside a module.
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

## UI
- Before writing or changing UI (any composable, including a fix inside one) or adding to `presentation-core`, load the `ui-kit` skill, and the `adaptive-layout` skill for screens, lists, grids, panes and window sizes.

## Toolchain
- Android SDK/NDK and Java compatibility come from `gradle/katari.versions.toml` plus build logic; do not hardcode them per module.
- The `libmpv` pin, its `gradle/verification-metadata.xml` entry and the `pa2x2/mpv-android` fork that releases it are upgraded only through the `mpv-fork-upgrade` skill (`.agents/skills/mpv-fork-upgrade`). A dependency audit of Katari lists them as available and leaves them to it; when the user asks for every dependency, include that skill's report groups unchanged.
- Do not use `--quiet` for compilation, tests, lint, or assembly because it suppresses warnings. Run Gradle with `--console=plain --warning-mode=all`, redirect the complete output to a unique file under `/tmp`, preserve the Gradle exit code, and inspect only diagnostic matches and nearby context (`w:`, `warning`, `e:`, `error`, `deprecated`, `FAILURE`, `What went wrong`, and `Caused by`). Match case-insensitively: the manifest merger and other tools print `Warning:`, and Gradle deprecations only say `has been deprecated`. If the build fails, inspect additional portions of the saved log selectively instead of printing the complete log. Use `--quiet` only for tasks whose diagnostic output is irrelevant.

## Validation
- Run `./gradlew spotlessApply` from the repo root.
- App unit tests run on the `foss` buildType (`testBuildType = "foss"`); library modules run theirs on `release` (the variant the app consumes, so tests reuse its compilation) and multiplatform modules through `allTests`. Focused example: `./gradlew :app:testFossUnitTest --tests '...'`.
- `./gradlew verifyTests --console=plain --warning-mode=all` runs every module's host tests and build-logic tests and compiles every device test. Every change that touches production or test code must pass it; `testFossUnitTest` and other module-specific test tasks cover a single module and leave failures elsewhere unnoticed. Modules join `verifyTests` through the convention plugins, so do not add per-module test tasks to aggregate verification lists.
- FOSS compilation can be verified with `:app:compileFossKotlin`; telemetry-enabled compilation uses `:app:compileReleaseKotlin :desktop:compileKotlin -Pinclude-telemetry`.
- Never combine FOSS/unit/architecture tasks with `-Pinclude-telemetry` in the same Gradle invocation. The property puts PostHog into every variant, the FOSS one included, and makes assembling a minified variant upload its R8 mapping to PostHog. Run FOSS checks without it, let that invocation finish, then run telemetry-enabled compilation in a separate invocation.
- Do not infer the installable variant for emulator/device validation from the `foss` unit-test buildType. Before installing, identify the package that is actually running and use the matching Gradle variant: `installDebug` installs `app.katari.dev`, while `installFoss` installs the separate `app.katari.foss` application. After installation, verify that the intended package was launched and that its process changed or restarted; installing a different application ID does not update the app under test.
- Never launch apps on a device with `adb shell monkey`: its random event can be a rotation, which turns the device's auto-rotate on. Use `adb shell am start -n app.katari.dev/eu.kanade.tachiyomi.ui.main.MainActivity`, or for another package `adb shell 'am start -n "$(cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER <package> | tail -n 1)"'`.
- After touching `core/data/src/commonMain/sqldelight`, run `./gradlew verifySqlDelightMigration`.

## Guidance
- When asked to fix the issue - never simply apply the easiest fix without finding the reason of the issue. Band-aid solutions are not welcomed. The goal is to fix the reason issue arised in the first place, not to merely fix the symptom
- Introduced warnings must not be left un-addressed. Not just suppressed so that thwy no longer show up, but cause of their appearance should be fixed instead
- Kotlin compilation treats warnings as errors (`allWarningsAsErrors` in build-logic). Do not disable it or suppress a warning to get past it; fix what causes the warning.
