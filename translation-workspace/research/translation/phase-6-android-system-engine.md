# Phase 6 Android system engine

Date: 2026-07-28

## Platform boundary

Android system translation is an installed Translation engine in every build, but runtime readiness begins at API 31
and remains device/OEM dependent. The provider-neutral engine owns product state mapping. A narrow Android wrapper owns
`TranslationManager`, `TranslationCapability`, `Translator`, `CancellationSignal`, and `PendingIntent`; none of those
types cross the Translation SPI or API.

`getOnDeviceTranslationCapabilities()` is called on `Dispatchers.IO`. Android explicitly documents that the query can
take several seconds and must run on a worker thread:
<https://developer.android.com/reference/android/view/translation/TranslationManager#getOnDeviceTranslationCapabilities(int,int)>.

The capability states map as follows:

- `STATE_ON_DEVICE` -> ready;
- `STATE_AVAILABLE_TO_DOWNLOAD` plus a settings intent -> system setup required;
- `STATE_AVAILABLE_TO_DOWNLOAD` without a settings intent -> system settings unavailable;
- `STATE_DOWNLOADING` -> setup in progress;
- `STATE_NOT_AVAILABLE`, unknown states, and an absent pair -> unsupported language pair.

Android documents `STATE_NOT_AVAILABLE` as an update-only state when existing support is dropped:
<https://developer.android.com/reference/android/view/translation/TranslationCapability#STATE_NOT_AVAILABLE>.

## Pair matching

Capability matching first requires an exact canonical BCP-47 pair. A regional request may then use a language-only
capability offered by the system, such as `en-US` using `en`. Katari does not guess across two distinct regional
capabilities: `pt-PT` does not use an advertised `pt-BR` capability. Translation context construction uses the exact
source and target specifications selected from the provider capability rather than reconstructing the requested tags.

## Preparation and execution lifecycle

Preparation stores only the selected text and provider-neutral language pair. It does not create a `Translator`,
register a listener, or retain an Android service object in a ready handle. This means a caller can abandon a prepared
translation without leaking provider resources.

Execution:

1. re-queries the pair capability;
2. registers a capability listener for that exact selected pair;
3. creates the translator asynchronously;
4. sends one text request with `FLAG_TRANSLATION_RESULT`;
5. races the response against a capability-loss update;
6. propagates coroutine cancellation through the provider cancellation signal;
7. unregisters the listener and destroys the translator in `finally`.

Android exposes explicit listener removal and a cancellation signal for active translation:
<https://developer.android.com/reference/android/view/translation/TranslationManager#removeOnDeviceTranslationCapabilityUpdateListener(java.util.function.Consumer)>,
<https://developer.android.com/reference/android/view/translation/Translator#translate(android.view.translation.TranslationRequest,android.os.CancellationSignal,java.util.concurrent.Executor,java.util.function.Consumer)>.
The request omits partial-response flags, so exactly one final standard translation response is expected:
<https://developer.android.com/reference/android/view/translation/TranslationRequest#FLAG_TRANSLATION_RESULT>.

Capability loss cancels the active operation and returns the new preparation state. Translator creation failure,
response failure, context rejection, and Android runtime exceptions remain attributed to this engine and never cause
automatic fallback. Provider exception messages are not exposed because they could contain implementation detail or
selected text.

## OEM setup

The engine opens only the immutable `PendingIntent` supplied by
`getOnDeviceTranslationSettingsActivityIntent()`. Android documents `null` when the OEM has no translation service or
the service supplies no settings:
<https://developer.android.com/reference/android/view/translation/TranslationManager#getOnDeviceTranslationSettingsActivityIntent()>.
Katari does not manufacture model download/deletion controls for system-owned models. Setup results distinguish a
missing service, missing settings, a stale/cancelled intent, and a successfully opened settings surface.
