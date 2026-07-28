# Phase 5 preferences, detection, and resolution

Date: 2026-07-28

## Variant-aware runtime participation

Optional implementations use a general Application Feature runtime-component mechanism:

- owner-local `*.application-feature-runtime-component` descriptors;
- direct generated Kotlin references for the active app variant;
- stable component IDs and typed queries through `ApplicationFeatureRuntimeInstallationContext`;
- no reflection, `ServiceLoader`, runtime source parsing, or central production component list.

The ML Kit descriptor and app dependency exist only for debug, release, preview, and benchmark. FOSS generates an
empty component registry and has no dependency on `:translation:mlkit`. This is compile-time app composition, not a
third-party plugin system.

Application Feature installation now receives generic base-store and profile-owner host dependencies. Translation
registers its preference owner within its own runtime module rather than adding a Translation-owned factory to the app
preference module.

## Preference ownership

Profile-owned:

- engine selection, defaulting to Automatic;
- explicit target language, defaulting dynamically to the effective AppCompat application/system locale;
- a false-by-default future reader automatic-selection opt-in, with no UI or behavior yet.

Device-owned:

- Wi-Fi-only model downloads, defaulting to true;
- ML Kit disclosure acknowledgement, stored as device app state.

There are no source, input, result, or history preferences.

## Detection

Android API 29 and newer use `TextClassifier.detectLanguage(TextLanguage.Request)`. Android documents the API as
added in API 29, blocking, potentially several seconds, and requiring a worker thread:
<https://developer.android.com/reference/android/view/textclassifier/TextClassifier.html#detectLanguage(android.view.textclassifier.TextLanguage.Request)>.

Standard API 26 through 28 uses the bundled `com.google.mlkit:language-id:17.0.6` artifact:
<https://developers.google.com/ml-kit/language/identification/android>.
ML Kit returns BCP-47 or `und`, accepts calls from any thread, and internally limits identification input to 200
characters:
<https://developers.google.com/android/reference/com/google/mlkit/nl/languageid/LanguageIdentifier>.
Each client is closed in `finally`, including cancellation and failure paths, as required by the client lifecycle
contract:
<https://developers.google.com/android/reference/com/google/mlkit/nl/languageid/LanguageIdentification>.

Provider tags are normalized through `TranslationLanguageTag`; missing, invalid, or `und` output becomes
`SourceUndetermined`. Detector failures are typed as unavailable, while coroutine cancellation propagates.

## Engine resolution

An engine supplies independent ready and setup priorities through the SPI. This keeps the resolver provider-neutral
while expressing the product policy:

1. prepare every eligible automatic candidate;
2. choose a ready candidate before every setup or unavailable candidate;
3. use ready priority to prefer Android system when both engines are ready;
4. use setup priority to prefer the ML Kit setup path when neither is ready;
5. retain registry order only as a deterministic equal-priority tie-break.

An explicit request wins over the saved profile selection. A saved explicit engine that is absent produces
`SelectedEngineUnavailable` and is not changed. Explicit engines never fall back. Provider limits are evaluated before
preparation; the shared 10,000-code-point ceiling remains absolute.

Immediately before execution, the Feature checks registry identity and calls the provider's explicit
`revalidate(ready)` operation. This keeps lifecycle authority with the provider instead of abandoning a
provider-owned ready handle. A changed state becomes `PreparationChanged`; a provider failure is returned and never
retried through another engine.
