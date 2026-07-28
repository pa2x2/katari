# Phase 6 Android system engine validation

Date: 2026-07-28

## Focused behavior

- `:translation:runtime:testDebugUnitTest`: 34 tests passed across the existing Translation runtime and 10 new Android
  system engine tests.
- `:translation:api:testDebugUnitTest`: 2 tests passed.
- The new tests cover all system capability mappings, typed API/service/settings/pair/runtime failure states, ready
  request revalidation, execution state changes, setup routing, exact and safe language-only capability matching,
  successful cleanup, capability-loss cancellation, unrelated capability updates, and API/service absence.
- Android wrapper fakes verify that active capability listeners are released and created translators are destroyed.

## Architecture and app builds

- `spotlessApply`: passed.
- `spotlessCheck`: passed.
- `verifyFeatureArchitecture`: passed.
- Production report remains one application subject, three Entry content types, 45 Features, 370 evaluated
  integrations, and zero obligations. Translation remains applicable with the Android system engine installed.
- `:app:compileFossKotlin`: passed.
- `:app:compileReleaseKotlin -Pinclude-telemetry`: passed in a separate invocation.
- `git diff --check`: passed.

The first architecture run exposed a stale validation scenario: it derived its empty-engine expectation from the
production registry, which is no longer empty after this phase. The contributor now constructs an explicit empty
registry, preserving the `NoEngineAvailable` contract independently of production composition.

No emulator or physical device was used.

## Documentation

Phase 6 platform research and lifecycle decisions are recorded in
`translation-workspace/research/translation/phase-6-android-system-engine.md`. User-facing Translation documentation
remains Phase 10 because no user-operable Translation surface exists yet.
