# Phase 9 Translation settings and test consumer validation

Date: 2026-07-28

## Automated checks

Passed:

- `./gradlew --quiet spotlessApply`
- `./gradlew --quiet spotlessCheck`
- `./gradlew --quiet verifyFeatureArchitecture`
- `./gradlew --quiet :translation:runtime:testDebugUnitTest :translation:ui:testDebugUnitTest`
- full `./gradlew --quiet :app:testFossUnitTest`
- `./gradlew --quiet :app:compileFossKotlin`
- separate `./gradlew --quiet :app:compileReleaseKotlin -Pinclude-telemetry`
- `git diff --check`

The language-catalog test protects the provider-neutral language picker rule: region-only locale variants collapse to
one base language, explicit script variants remain distinct, and indeterminate locales are excluded.

## Authorized emulator verification

Device:

- `sdk_gphone64_x86_64`
- Android 16 / API 36
- installed and launched matching debug package `app.katari.dev`

Verified:

- Translation appears as a top-level Settings destination.
- The Translation screen identifies its profile-owned Configuration group.
- Engine selection exposes Automatic and the Android system on-device engine.
- The searchable target picker exposes the effective app language and normalized BCP-47 choices.
- `Test translation` uses a focused transient text dialog and explains its session-only lifetime.
- A short French sample submitted through the production `TranslationFeature` returned the real typed
  `SystemSetupRequired(LanguageModelsRequired)` state.
- The shared adaptive sheet rendered the precise setup reason and Settings action.
- The Settings action launched the exact OEM `PendingIntent` destination, the device's Live Translate settings.
- Returning to Katari re-prepared the transient request and retained the accurate setup-required state.
- Dismissal/navigation cleared the session. A read-only app-storage and logcat scan found no submitted source text.
- No language download or OEM setting change was initiated.

Android 16 initially rejected the OEM `PendingIntent` activity launch because the sender had not opted into the
platform's background-activity-start policy. The bridge now supplies `ALLOW_IF_VISIBLE` on API 36+ and the legacy
`ALLOWED` mode on API 34-35. The successful rerun reported `BAL_ALLOW_VISIBLE_WINDOW` and opened the OEM activity.

An unrelated pre-existing extension installation left `ExtensionInstallService` running behind a Play Protect
prompt and produced a short-service timeout dialog during the first attempt. ActivityManager attributed that ANR to
the extension service, not Translation. Dismissing the stale system flow and rerunning produced the successful
Translation evidence above.

The Settings destination, language and engine pickers, transient input, setup sheet, and OEM destination were
inspected live. Screenshots are intentionally not retained in the workspace.

## Privacy and ownership inspection

- The test input uses `remember`, not saveable state.
- The input is passed directly to `TranslationSessionController`.
- The controller is owned by a Voyager screen model and closes with its scope.
- Only `ProfileTranslationPreferences.engineSelection` and `.targetLanguage` are written.
- No source, result, history, navigation-argument, log, analytics, or database persistence was added.

## Documentation

User documentation remains intentionally scheduled for Phase 10 now that the production Settings surface and its
real Android/OEM behavior are established.
