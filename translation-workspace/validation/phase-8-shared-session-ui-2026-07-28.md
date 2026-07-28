# Phase 8 shared Translation session UI validation

Date: 2026-07-28

## Controller and layout behavior

- `:translation:ui:testDebugUnitTest`: 12 tests passed.
- Seven controller tests protect 250 ms latest-selection settling, automatic immediate execution, explicit invocation,
  non-cooperative stale-result suppression, anchor-only updates without repeated provider work, no automatic execution
  loop after a preparation change, and dismissal text clearing.
- Five placement tests protect below/above positioning, horizontal safe-bound clamping, measured overflow promotion,
  and invalid/off-viewport anchor rejection.
- The controller catches provider exceptions without converting coroutine cancellation, revalidates the current
  generation before publishing, and clears state when its owner scope ends.

## Compose and emulator behavior

- `:translation:ui:connectedDebugAndroidTest`: 3 tests passed on the authorized
  `sdk_gphone64_x86_64` Android 16/API 36 emulator.
- The successful-result fixture rendered through the anchored popup.
- Missing anchor plus source-language choice rendered through the adaptive bottom sheet.
- A long measured result promoted from popup eligibility to the adaptive sheet without a character-count heuristic.
- The instrumentation assertions use semantic surface tags and visible content, not pixel snapshots.

Visual inspection captures from the emulator:

- [anchored result popup](phase-8-screenshots/phase-8-translation-popup.png)
- [source-language adaptive sheet](phase-8-screenshots/phase-8-translation-sheet.png)

The captures contain fixed test strings only and no private or real selected text.

## Architecture, build, and formatting evidence

- `./gradlew spotlessApply spotlessCheck :translation:ui:testDebugUnitTest :translation:ui:compileReleaseKotlin
  verifyFeatureArchitecture :app:compileFossKotlin --quiet` — passed.
- `./gradlew :translation:ui:connectedDebugAndroidTest --quiet` — passed separately on the emulator.
- `./gradlew :app:compileReleaseKotlin -Pinclude-telemetry --quiet` — passed separately.
- `git diff --check` — passed.

The UI depends on `TranslationFeature` and API metadata. System setup, language-picker, profile-default, model, and
documentation operations leave the shared UI as typed external actions for the Phase 9 host; no reader needs
Translation SPI.

## Documentation

No public user documentation was changed because the Translation UI still has no production navigation destination
or reader adapter. User documentation remains Phase 10.

No physical device was used.
