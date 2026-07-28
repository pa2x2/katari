# Phase 4 Translation contracts validation

Date: 2026-07-28

Refactor prerequisite commit: `ecbf752e8` (`(refactor): host feature reporting in app`)

## Architecture and contract evidence

- `./gradlew verifyFeatureArchitecture --quiet` — passed.
- `:translation:api` — 2 tests passed.
- `:translation:runtime` — 6 tests passed.
- `:feature-runtime` — 6 tests passed, including application graph-validator execution.
- Fake engines exercised provider-neutral preparation, success, readiness changes, opaque-handle rejection, registry
  revalidation, Unicode code-point limits, and coroutine cancellation.
- The reusable Translation contract validator was discovered by the app-owned production report.

The generated production report contains:

- one application subject;
- three Entry content types;
- 45 Features;
- 22 execution points;
- 370 evaluated integrations;
- zero obligations;
- an applicable `translation/translation.engine-registry` application integration;
- a passed `translation.behavior` contract.

## Build and formatting evidence

- `:translation:mlkit:compileDebugKotlin` — passed with the provider implementation intentionally deferred.
- `:translation:ui:compileDebugKotlin` — passed with UI implementation intentionally deferred.
- `:app:compileFossKotlin` — passed.
- `:app:compileReleaseKotlin -Pinclude-telemetry` — passed in a separate invocation.
- `./gradlew spotlessApply --quiet` — passed.
- `./gradlew spotlessCheck --quiet` — passed.
- `git diff --check` — passed.

No user-facing documentation was changed because Phase 4 introduces internal contracts and no usable engine or UI.
Translation user documentation remains an explicit later phase.

No emulator or physical device was used.
