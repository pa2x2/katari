# Translation Feature manifesto

## Product promise

Katari provides one app-wide Translation Feature that any current or future surface can consume. Readers provide
selected text and an optional visual anchor; they do not know which translation engines are installed, ready,
preferred, or supported by the device.

The first implementation uses Android system translation. A ready system translation may produce an anchored result
immediately. Devices without the required Android service, OS support, language pair, or setup action receive a
precise typed state instead of a hidden fallback.

## Scope of the first implementation

Included:

- app-wide Translation Feature API;
- internal engine SPI;
- Android system translation;
- Android platform source-language detection where available;
- profile-owned engine and target preferences;
- reusable anchored popup and adaptive setup/result sheet;
- top-level Translation settings;
- an end-to-end Settings playground consumer;
- user documentation for platform and OEM limitations.

Deferred:

- HTML prose selection integration;
- Readium selection integration;
- manga OCR;
- bundled third-party translation or detection SDKs;
- external translator-app handoff;
- cloud translation APIs and user API keys;
- Katari/community model-package distribution;
- source-extension translation APIs;
- translation history;
- automatic chunking, batch translation, and background translation.

Do not allow deferred work to distort the first API. Reader and OCR mechanics remain adapters around the app-wide
Feature.

## Build and provider policy

Every build contains the same Translation Feature and Android system engine adapter. The first implementation has no
bundled third-party translation engine or language-detection SDK.

The API and SPI remain provider-neutral so a future engine can be added behind the Feature without changing readers
or other consumers. Adding any provider implementation is a separate product and dependency decision; generic
contracts do not authorize bundling one.

Installed translator apps are not inline engines. Android text-processing intents are external handoff and remain
explicitly deferred.

## Provider resolution

Every profile stores one explicit engine ID.

Resolution rules:

1. The Android system engine is the default and only installed engine in the first implementation.
2. A request uses its explicit override or the profile's explicit engine.
3. An unavailable engine never silently falls back.
4. A runtime provider failure never silently retries another provider.
5. If a saved engine is absent on another build or device, ask the user to choose; preserve the stored
   preference until they do.

The static Feature Graph declares that an engine registry exists. Per-device services, language-pair support, setup,
and user choices are runtime Translation Feature states.

## Language behavior

- Language identities are normalized BCP-47 tags, never provider enums.
- Source language is detected automatically when the Android platform detector is available.
- API 29 and newer use Android `TextClassifier` off the main thread.
- When platform detection is unavailable or genuinely undetermined, open a per-request source chooser.
- The target defaults to Katari's effective UI locale while the profile has no explicit override.
- If source equals target, ask for a target for the current request without changing the profile default.
- Source and target can both be overridden for one request.
- A profile default changes only through an explicit `Use as default` action.
- Mixed-language selections are treated as one detected language in the first version.

## Invocation and presentation

Provider presentation is data returned by the provider/Feature, not UI knowledge inferred from engine IDs.

Presentation metadata includes:

- localized provider and engine names;
- immediate or explicit-labeled invocation policy;
- optional provider disclosure;
- optional result attribution and asset reference;
- optional documentation/privacy link.

Android system translation uses immediate invocation when ready. Future providers must declare their own invocation
and presentation requirements through the existing contracts; the shared UI must render those contracts without
provider-specific branches.

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

No network translation engine is permitted in this scope. Katari does not own system language-data downloads; Android
or the OEM owns any service setup and network behavior outside Katari's process.

## Model and setup behavior

- Android/OEM model management stays in system settings when the OEM exposes an intent.
- Do not manufacture a Katari deletion or download UI for system models Katari cannot control.
- Generic model-management contracts remain reserved for a future provider that can actually implement them.
- Do not add model preferences, inventory, or download controls before such a provider exists.
- Close translators, listeners, and cancellation resources deterministically.

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

Each provider declares a safe input limit. Apply a defensive shared ceiling of 10,000 Unicode code points when no
lower provider limit exists. Reject oversized input with guidance; never truncate or chunk invisibly.

Coroutine cancellation is cancellation, not a provider error. Changing selection cancels the previous session.

## UI behavior

`TranslationSessionController` owns a latest-selection-wins state machine:

- wait 250 ms after selection movement settles;
- cancel prior preparation/translation;
- clear when the selection, document, or host disappears.
- follow provider invocation policy in readers;
- automatically recheck setup while a provider reports progress;
- debounce playground requests and automatically execute them only when the provider declares immediate invocation.

Use an anchored, non-modal popup when an anchor exists and content fits. Use measured layout overflow, not a character
threshold. Promote to the adaptive sheet for:

- missing/unsafe anchor;
- overflowing result;
- provider disclosure;
- system setup;
- language or engine choice;
- errors requiring action.

The popup exposes translation, language pair, optional provider attribution, copy, expand, language change, and close.

No reader adapter ships in the first slice. Translation Settings supplies the first real consumer through an inline,
transient playground. Its embedded setup, progress, failure, execution, and result panel shares the same renderer as
the reader popup/sheet host.

Future reader behavior is profile opt-in and off by default. Do not expose that preference before a reader adapter
exists.

## Settings ownership

Translation is a top-level Settings category, not a child of Readers or Appearance.

Profile-owned:

- explicit engine;
- explicit target language.

The first implementation has no Translation-owned device-wide preferences.

Settings must show precise reasons such as:

- unsupported Android version;
- OEM service missing;
- system setup required;
- setup in progress;
- unsupported language pair.

Do not collapse these into a generic unavailable state.

## Documentation obligations

User documentation must explain Android/OEM requirements, on-device processing, system-owned language data, profile
ownership, and lack of history.

These obligations are part of the feature, not optional release polish.

## Completion criteria

The first slice is complete only when:

- Settings can expose preparation and setup before execution, then exercise translation and result UI end to end;
- system-ready translation appears immediately;
- successful results carry the provider presentation returned by the Feature;
- profile preferences have correct ownership;
- no Translation-owned device preference exists without active behavior;
- no text/history persistence or logging exists;
- documentation describes the real platform and OEM limitations;
- all builds contain the same system-only Translation implementation;
- reader-specific and OCR code has not leaked into the Translation API.
