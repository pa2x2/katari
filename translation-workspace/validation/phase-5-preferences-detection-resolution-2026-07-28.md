# Phase 5 preferences, detection, and resolution validation

Date: 2026-07-28

## Focused behavior

- `:translation:runtime:testDebugUnitTest`: 24 tests passed across Feature resolution, profile/device preferences,
  default target resolution, Android detection, component detector aggregation, and graph installation.
- `:translation:mlkit:testDebugUnitTest`: 3 tests passed for BCP-47, undetermined output, and cancellation.
- `:feature-runtime:testDebugUnitTest`: 8 tests passed, including typed runtime-component lookup and duplicate IDs.
- Build-logic focused tests: 13 tests passed for component generation and owner-local boundary rules.

The resolver coverage proves ready-first priority, system-ready preference, ML Kit setup preference, explicit
no-fallback behavior, saved-absent behavior, source-undetermined and source-equals-target outcomes, stale-handle
revalidation, provider limits, cancellation, and no retry after provider failure.

## Architecture and app builds

- `spotlessApply`: passed.
- `spotlessCheck`: passed.
- `verifyFeatureArchitecture`: passed.
- Production report remains one application subject, three Entry content types, 45 Features, 370 evaluated
  integrations, and zero obligations. Translation remains applicable.
- `:app:compileFossKotlin`: passed.
- `:app:compileReleaseKotlin -Pinclude-telemetry`: passed in a separate invocation.
- `git diff --check`: passed.

Generated topology inspection:

- FOSS `productionApplicationFeatureRuntimeComponents()` contains no registrations.
- Debug contains one direct registration for
  `mihon.translation.mlkit.mlKitTranslationRuntimeComponent`.

Runtime classpath inspection:

- `fossRuntimeClasspath`: no `com.google.mlkit` or `language-id` match.
- `debugRuntimeClasspath`: `com.google.mlkit:language-id:17.0.6` with its ML Kit common dependencies.

No emulator or physical device was used.

## Documentation

The developer Feature architecture documentation now covers owner-local profile preference installation and
variant-aware runtime components. User-facing Translation documentation remains Phase 10 because no user-operable
Translation surface exists yet.
