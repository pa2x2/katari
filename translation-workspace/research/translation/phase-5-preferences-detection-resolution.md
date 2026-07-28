# Phase 5 preferences, detection, and resolution

Date: 2026-07-28

## Runtime participation

Optional implementations can use the general Application Feature runtime-component mechanism:

- owner-local `*.application-feature-runtime-component` descriptors;
- direct generated Kotlin references for the active app variant;
- stable component IDs and typed queries through `ApplicationFeatureRuntimeInstallationContext`;
- no reflection, `ServiceLoader`, runtime source parsing, or central production component list.

The first Translation slice has no optional component. Every variant installs the Android system engine directly from
the Translation runtime. The component seam remains provider-neutral infrastructure for a separately approved future
implementation.

Application Feature installation receives the profile-owner host dependency. Translation registers its preference
owner within its own runtime module rather than adding a Translation-owned factory to the app preference module.

## Preference ownership

Profile-owned:

- one explicit engine ID, defaulting to the bundled Android system engine;
- explicit target language, defaulting dynamically to the effective AppCompat application/system locale;
- a false-by-default future reader automatic-selection opt-in, with no UI or behavior yet.

There are no device-wide Translation preferences and no source, input, result, or history preferences.

## Detection

Android API 29 and newer use `TextClassifier.detectLanguage(TextLanguage.Request)`. Android documents the API as
added in API 29, blocking, potentially several seconds, and requiring a worker thread:
<https://developer.android.com/reference/android/view/textclassifier/TextClassifier.html#detectLanguage(android.view.textclassifier.TextLanguage.Request)>.

Provider tags are normalized through `TranslationLanguageTag`; missing, invalid, or `und` output becomes
`SourceUndetermined`. Detector failures are typed as unavailable, while coroutine cancellation propagates.

When the Android platform detector is unavailable, the detector list is empty and an automatic-source request returns
`SourceUndetermined`. The consumer must offer explicit source-language selection; the runtime does not guess.

## Engine resolution

A request's explicit override wins over the saved profile engine. Otherwise, the resolver uses the saved engine ID.
The registry resolves only that exact engine; there is no candidate ranking or fallback.

A saved engine that is absent produces `SelectedEngineUnavailable` and is not changed. Provider limits are evaluated
before preparation; the shared 10,000-code-point ceiling remains absolute.

Immediately before execution, the Feature checks registry identity and calls the provider's explicit
`revalidate(ready)` operation. This keeps lifecycle authority with the provider instead of abandoning a
provider-owned ready handle. A changed state becomes `PreparationChanged`; a provider failure is returned and never
retried through another engine.
