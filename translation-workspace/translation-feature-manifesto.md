# Translation Feature manifesto

## Product promise

Katari provides one app-wide Translation Feature that any current or future surface can consume. Readers provide
selected text and an optional visual anchor; they do not know which translation engines are built, installed, ready,
downloaded, preferred, or legally constrained.

The intended reader experience is an automatic, anchored translation popup after selection settles. Android system
translation may produce the result immediately. Google ML Kit requires one in-popup `Translate with Google` action
before execution.

## Scope of the first implementation

Included:

- app-wide Translation Feature API;
- internal engine SPI;
- Android system translation;
- standard-build-only Google ML Kit translation;
- automatic source-language detection;
- profile preferences and device-wide model policy;
- model download, inventory, pre-download, and deletion;
- reusable anchored popup and adaptive setup/result sheet;
- top-level Translation settings;
- an end-to-end `Test translation` consumer;
- user documentation, privacy disclosure, and release-template warning.

Deferred:

- HTML prose selection integration;
- Readium selection integration;
- manga OCR;
- external translator-app handoff;
- cloud translation APIs and user API keys;
- Katari/community model-package distribution;
- source-extension translation APIs;
- translation history;
- automatic chunking, batch translation, and background translation.

Do not allow deferred work to distort the first API. Reader and OCR mechanics remain adapters around the app-wide
Feature.

## Build and provider policy

Every build contains the Translation Feature and Android system engine adapter.

Standard, debug, preview, and benchmark builds additionally contain Google ML Kit. FOSS must contain no ML Kit
dependency, implementation class, resource, or transitive runtime artifact.

Settings still lists all known engines. In FOSS, Google ML Kit appears disabled as `Not included in this build`, with
a link explaining build and privacy differences. Describing a known engine must not load or package its implementation.

Installed translator apps are not inline engines. Android text-processing intents are external handoff and are
explicitly deferred.

## Provider resolution

Profile engine selection defaults to `Automatic`.

Automatic resolution rules:

1. A ready engine beats one requiring setup.
2. If Android system translation and ML Kit are both ready, Android system wins to preserve the immediate result.
3. If neither is ready in the standard build, prefer the ML Kit setup path.
4. FOSS resolves only the Android system path.
5. An explicitly selected engine never silently falls back.
6. A runtime provider failure never silently retries another provider.
7. If an explicitly saved engine is absent on another build or device, ask the user to choose; preserve the stored
   preference until they do.

The static Feature Graph declares that an engine registry exists. Per-device services, model state, language-pair
support, connectivity, and user choices are runtime Translation Feature states.

## Language behavior

- Language identities are normalized BCP-47 tags, never provider enums.
- Source language is detected automatically.
- API 29 and newer use Android `TextClassifier` off the main thread.
- Standard API 26 through 28 uses bundled ML Kit language identification.
- A genuinely undetermined source opens a per-request source chooser.
- The target defaults to Katari's effective UI locale while the profile has no explicit override.
- If source equals target, ask for a target for the current request without changing the profile default.
- Source and target can both be overridden for one request.
- A profile default changes only through an explicit `Use as default` action.
- Mixed-language selections are treated as one detected language in the first version.

## Invocation and attribution

Provider presentation is data returned by the provider/Feature, not UI knowledge inferred from engine IDs.

Presentation metadata includes:

- localized provider and engine names;
- immediate or explicit-labeled invocation policy;
- provider disclosure;
- result attribution;
- official attribution asset reference;
- documentation/privacy link.

Android system translation uses immediate invocation when ready.

ML Kit uses:

- an explicit `Translate with Google` action inside the already-open popup;
- official Google Translate attribution adjacent to every result;
- the required disclaimer available in the UI and documentation.

A result label such as `Translated with Google` does not replace the required triggering action. Do not automatically
execute ML Kit merely because the popup can display attribution afterward.

Do not place a Google logo next to competing provider logos in Settings. Textual engine identification is sufficient
there; provider branding belongs beside the active provider result.

## Privacy and persistence

Source text and translated text are session memory only.

Never write them to:

- preferences;
- databases;
- backups;
- analytics;
- crash metadata;
- logs;
- this workspace.

The standard build must disclose before first ML Kit model download that:

- translation input and output processing is on-device;
- model files are downloaded from Google;
- the SDK reports its documented diagnostics and usage metadata;
- Google attribution appears with results.

Disclosure acknowledgement is device-wide. Engine and target choices are profile-specific. Model files and Wi-Fi
download policy are device-wide.

No network translation engine is permitted in this scope. Model downloads are the only provider network operation
owned by Translation.

## Model behavior

- Wi-Fi-only downloads default to enabled.
- The inline setup flow may offer an explicit one-download mobile-data override.
- That override does not alter the global Wi-Fi policy.
- Display the missing languages and approximate size before downloading.
- ML Kit estimates use approximately 30 MB per missing language model.
- Settings lists installed models and supports pre-download and deletion.
- Android/OEM model management stays in system settings when the OEM exposes an intent.
- Do not manufacture a Katari deletion or download UI for system models Katari cannot control.
- Close translators, detectors, listeners, and cancellation resources deterministically.

## API behavior

Consumers use `TranslationFeature.prepare(request)` followed by `translate(ready)` only when invocation policy permits.

Preparation returns typed states for:

- ready;
- provider disclosure;
- model download;
- system setup;
- setup in progress;
- source undetermined;
- source equals target;
- saved engine unavailable;
- unsupported OS/service/language pair;
- blank or oversized input.

Prepared execution handles are opaque and process-local. Revalidate provider readiness before execution. If state
changed, return the latest setup or unavailable state rather than running a stale handle.

Each provider declares a safe input limit. Apply a defensive shared ceiling of 10,000 Unicode code points when no lower
provider limit exists. Reject oversized input with guidance; never truncate or chunk invisibly.

Coroutine cancellation is cancellation, not a provider error. Changing selection cancels the previous session.

## UI behavior

`TranslationSessionController` owns a latest-selection-wins state machine:

- wait 250 ms after selection movement settles;
- cancel prior preparation/translation;
- clear when the selection, document, or host disappears.

Use an anchored, non-modal popup when an anchor exists and content fits. Use measured layout overflow, not a character
threshold. Promote to the adaptive sheet for:

- missing/unsafe anchor;
- overflowing result;
- provider disclosure;
- model/system setup;
- language or engine choice;
- errors requiring action.

The popup exposes translation, language pair, provider attribution, copy, expand, language change, and close.

No reader adapter ships in the first slice. Translation Settings supplies the first real consumer through a transient
`Test translation` flow using the same controller and UI.

Future reader behavior is profile opt-in and off by default. Do not expose that preference before a reader adapter
exists.

## Settings ownership

Translation is a top-level Settings category, not a child of Readers or Appearance.

Profile-owned:

- Automatic or explicit engine;
- explicit target language.

Device-wide:

- Wi-Fi-only model downloads;
- ML Kit disclosure acknowledgement;
- model inventory.

Settings must show precise reasons such as:

- not included in this build;
- unsupported Android version;
- OEM service missing;
- model required;
- model downloading;
- unsupported language pair.

Do not collapse these into a generic unavailable state.

## Documentation and release obligations

The user documentation must explain engines, on-device processing, SDK diagnostics, model storage, profile/device
ownership, FOSS limitations, attribution, and lack of history.

The builds/privacy page must stop claiming that standard and FOSS contain identical media features.

Every generated GitHub release body must link to the build comparison and warn that:

- standard includes ML Kit;
- FOSS relies on Android/OEM system translation.

These obligations are part of the feature, not optional release polish.

## Completion criteria

The first slice is complete only when:

- Settings can exercise preparation, setup, translation, and result UI end to end;
- system-ready translation appears immediately;
- ML Kit cannot execute without its labeled action;
- every successful result carries provider metadata and required attribution;
- profile and global preferences have correct ownership;
- model management is observable and cancellable;
- FOSS has no ML Kit runtime dependency;
- no text/history persistence or logging exists;
- documentation and release templates describe the real build behavior;
- reader-specific and OCR code has not leaked into the Translation API.
