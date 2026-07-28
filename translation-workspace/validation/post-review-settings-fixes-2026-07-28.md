# Translation settings post-review fixes validation

Date: 2026-07-28

## Corrected invariants

- The session controller no longer has a mode that can bypass `ExplicitAction`; the playground follows provider policy
  and therefore auto-executes only `Immediate` providers.
- Engine labels use one universal `engine name · provider name` rule without inspecting concrete provider IDs.
- The custom Translation settings layout consumes Settings search targets, scrolls to the owning card, highlights the
  matching playground/default row, clears the global token after consumption, and also clears it if the screen leaves
  before consumption completes.
- The unreleased Automatic-engine migration path, migration-only test, and current planning claims were removed.

## Automated validation

Passed:

- `./gradlew spotlessApply --quiet`
- `./gradlew spotlessCheck verifyFeatureArchitecture :translation:api:test
  :translation:runtime:testDebugUnitTest :translation:ui:testDebugUnitTest :app:compileFossKotlin --quiet`
- focused Translation settings model and language-catalog FOSS unit tests
- `git diff --check`

## Authorized emulator verification

- Rebuilt and installed the matching `app.katari.dev` debug variant; the app process changed after installation.
- Settings search returned the Translation engine and Playground destinations.
- Opening the Translation engine result displayed the custom Translation screen with the defaults destination visible.
- Android uses the same provider-neutral label shape as every other engine:
  `System on-device translation · Android`.
- No screenshots or UI-dump artifacts were retained.

The first incremental `installDebug` packaging attempt failed without a nested diagnostic. A direct `packageDebug`
rerun succeeded, followed by a successful `installDebug`; the installed app was used for the checks above.
