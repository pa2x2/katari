package app.katari.tts.provider.speechdispatcher.engine

import app.katari.tts.api.engine.KnownTtsEngine
import app.katari.tts.api.engine.TtsEngineArtwork
import app.katari.tts.api.engine.TtsEngineBuildAvailability
import app.katari.tts.api.engine.TtsEngineDetails
import app.katari.tts.api.engine.TtsEngineId
import app.katari.tts.api.engine.TtsProviderId
import app.katari.tts.api.provider.TtsProviderPresentation
import app.katari.tts.provider.speechdispatcher.resources.Res
import app.katari.tts.provider.speechdispatcher.resources.ic_speech_dispatcher
import tachiyomi.i18n.*
import tachiyomi.i18n.localized.stringResource

internal val SPEECH_DISPATCHER_PROVIDER_ID = TtsProviderId("speech-dispatcher")

internal fun speechDispatcherCatalogEntry(): KnownTtsEngine = KnownTtsEngine(
    id = TtsEngineId("speech-dispatcher"),
    providerId = SPEECH_DISPATCHER_PROVIDER_ID,
    providerName = SPEECH_DISPATCHER_NAME,
    engineName = SPEECH_DISPATCHER_NAME,
    buildAvailability = TtsEngineBuildAvailability.Included,
    artwork = TtsEngineArtwork.Glyph(Res.drawable.ic_speech_dispatcher),
    details = TtsEngineDetails(
        description = stringResource(MR.strings.tts_speech_dispatcher_description),
        processingDescription = stringResource(MR.strings.tts_speech_dispatcher_processing),
        privacyDescription = stringResource(MR.strings.tts_speech_dispatcher_privacy),
    ),
    documentationUrl = SPEECH_DISPATCHER_DOCUMENTATION,
)

internal fun KnownTtsEngine.speechDispatcherPresentation(): TtsProviderPresentation = TtsProviderPresentation(
    providerId = providerId,
    providerName = providerName,
    engineName = engineName,
    documentationUrl = documentationUrl,
)

private const val SPEECH_DISPATCHER_NAME = "Speech Dispatcher"
private const val SPEECH_DISPATCHER_DOCUMENTATION = "https://github.com/brailcom/speechd"
