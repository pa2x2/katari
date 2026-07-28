# Phase 3 validation and reporting evidence

Date: 2026-07-27

Base commit: `0a11efb94` (`(refactor): share feature runtime composition`)

## Architecture gates

- `./gradlew verifyFeatureArchitecture --quiet` — passed.
- `./gradlew verifyEntryFeatureArchitecture --quiet` — passed as a compatibility alias.
- `./gradlew generateFeatureReport --quiet` — passed using the documented root task selector.
- The generated report contains:
  - one application subject;
  - three Entry content types;
  - 44 Features;
  - 22 execution points;
  - 369 evaluated Entry integrations;
  - zero obligations.

## Test evidence

- `feature-graph`: 70 tests passed across 11 suites.
- `feature-runtime`: 5 tests passed across 2 suites.
- `feature-validation`: 10 tests passed across 4 suites.
  - Includes application integration fixture selection and application execution-contract fixture selection.
  - Includes Entry-scoped optional projection filtering with an application-only Feature present.
- `entry-interactions:documentation`: 16 tests passed across 5 suites.
- `entry-interactions`: 235 tests passed across 65 suites.
- `gradle/build-logic`: 102 tests passed across 11 suites.

## Formatting and documentation

- `./gradlew spotlessApply --quiet` — passed.
- `./gradlew spotlessCheck --quiet` — passed.
- `corepack pnpm docs:build` — passed.
- `git diff --check` — passed.

The content-type reference and source SDK coverage verification passed through `verifyFeatureArchitecture`; application
subjects did not alter the checked-in Entry documentation.

No emulator or physical device was used.
