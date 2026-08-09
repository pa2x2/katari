package mihon.tts.provider.android.engine

import android.app.Application
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mihon.tts.api.engine.KnownTtsEngine
import mihon.tts.api.preparation.TtsSystemSetupReason
import mihon.tts.api.preparation.TtsUnavailableReason
import mihon.tts.api.provider.TtsInputLimit
import mihon.tts.api.provider.TtsOptionalCapability
import mihon.tts.api.provider.TtsParameterRange
import mihon.tts.api.provider.TtsParameterSupport
import mihon.tts.api.provider.TtsProviderCapabilities
import mihon.tts.api.provider.TtsProviderPresentation
import mihon.tts.api.request.ResolvedTtsRequest
import mihon.tts.api.voice.TtsVoiceInspection
import mihon.tts.provider.android.connection.AndroidTtsConnection
import mihon.tts.provider.android.discovery.ANDROID_TTS_PROVIDER_ID
import mihon.tts.provider.android.discovery.isAndroidTtsEngineInstalled
import mihon.tts.provider.android.voice.toApiVoice
import mihon.tts.spi.engine.ReadyTtsEngineRequest
import mihon.tts.spi.engine.TtsEngine
import mihon.tts.spi.engine.TtsEngineDeviceAvailability
import mihon.tts.spi.engine.TtsEngineExecution
import mihon.tts.spi.engine.TtsEnginePreparation

internal class AndroidTtsEngine(
    private val application: Application,
    private val enginePackage: String,
    override val catalogEntry: KnownTtsEngine,
    override val presentation: TtsProviderPresentation,
    private val blockingDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TtsEngine {
    private val connection = AndroidTtsConnection(
        application = application,
        enginePackage = enginePackage,
        blockingDispatcher = blockingDispatcher,
    )
    private val voiceInspectionMutex = Mutex()

    @Volatile
    private var voiceInspection: TtsVoiceInspection? = null

    override val capabilities = TtsProviderCapabilities(
        rangeProgress = TtsOptionalCapability.Supported,
        speechRate = TtsParameterSupport.Supported(TtsParameterRange(0.1f, 3f, 1f)),
        pitch = TtsParameterSupport.Supported(TtsParameterRange(0.5f, 2f, 1f)),
        inputLimit = TtsInputLimit.MaximumCodePoints(TextToSpeech.getMaxSpeechInputLength() / 2),
    )

    override suspend fun inspectDevice(): TtsEngineDeviceAvailability {
        return if (withContext(blockingDispatcher) {
                application.packageManager.isAndroidTtsEngineInstalled(enginePackage)
            }
        ) {
            TtsEngineDeviceAvailability.Available
        } else {
            TtsEngineDeviceAvailability.NotInstalled
        }
    }

    override suspend fun inspectVoices(): TtsVoiceInspection {
        voiceInspection?.let { return it }
        return inspectVoices(forceRefresh = false)
    }

    override suspend fun refreshVoices(): TtsVoiceInspection = inspectVoices(forceRefresh = true)

    private suspend fun inspectVoices(forceRefresh: Boolean): TtsVoiceInspection = voiceInspectionMutex.withLock {
        if (!forceRefresh) voiceInspection?.let { return@withLock it }
        voiceInspection = null
        if (inspectDevice() != TtsEngineDeviceAvailability.Available) {
            return TtsVoiceInspection.Unavailable(catalogEntry.id, "Android TTS engine is not installed")
        }
        return try {
            val snapshot = connection.voiceSnapshot(forceRefresh)
            val inspection = withContext(blockingDispatcher) {
                val voices = snapshot.voices.mapNotNull { voice ->
                    voice.toApiVoice(ANDROID_TTS_PROVIDER_ID, catalogEntry.id)
                }.sortedBy { it.name }
                if (voices.isEmpty()) {
                    TtsVoiceInspection.VoiceDataRequired(catalogEntry.id, "The engine reported no installed voices")
                } else {
                    val defaultVoice = snapshot.defaultVoice
                        ?.toApiVoice(ANDROID_TTS_PROVIDER_ID, catalogEntry.id)
                        ?.id
                        ?.takeIf { candidate -> voices.any { it.id == candidate } }
                    TtsVoiceInspection.Available(catalogEntry.id, voices, defaultVoice)
                }
            }
            voiceInspection = inspection
            inspection
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TtsVoiceInspection.Failed(catalogEntry.id, "Android TTS engine initialization failed")
        }
    }

    override suspend fun prepare(request: ResolvedTtsRequest): TtsEnginePreparation {
        if (inspectDevice() != TtsEngineDeviceAvailability.Available) {
            return TtsEnginePreparation.Unavailable(
                TtsUnavailableReason.EngineUnavailable(catalogEntry.id, "Android TTS engine is not installed"),
            )
        }
        return try {
            val voice = connection.resolveVoice(request.voice.id.value)
            if (voice != null) {
                TtsEnginePreparation.Ready(AndroidReadyTtsRequest(this, request, voice))
            } else {
                TtsEnginePreparation.SystemSetupRequired(TtsSystemSetupReason.VoiceDataRequired)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TtsEnginePreparation.Unavailable(
                TtsUnavailableReason.EngineUnavailable(catalogEntry.id, "Android TTS engine initialization failed"),
            )
        }
    }

    override suspend fun revalidate(ready: ReadyTtsEngineRequest): TtsEnginePreparation {
        val androidReady = ready as? AndroidReadyTtsRequest
            ?: return TtsEnginePreparation.Unavailable(
                TtsUnavailableReason.EngineUnavailable(catalogEntry.id, "Invalid Android TTS readiness"),
            )
        if (androidReady.owner !== this) {
            return TtsEnginePreparation.Unavailable(
                TtsUnavailableReason.EngineUnavailable(catalogEntry.id, "Stale Android TTS readiness"),
            )
        }
        val voice = connection.cachedVoice(androidReady.request.voice.id.value)
            ?: return TtsEnginePreparation.SystemSetupRequired(TtsSystemSetupReason.VoiceDataRequired)
        return TtsEnginePreparation.Ready(androidReady.copy(voice = voice))
    }

    override suspend fun play(ready: ReadyTtsEngineRequest): TtsEngineExecution {
        val androidReady = ready as? AndroidReadyTtsRequest
            ?: return TtsEngineExecution.Failed("Invalid Android TTS readiness")
        if (androidReady.owner !== this) return TtsEngineExecution.Failed("Stale Android TTS readiness")
        return try {
            connection.play(androidReady.request, androidReady.voice)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TtsEngineExecution.Failed("Android TTS playback failed to start")
        }
    }

    override suspend fun release() {
        voiceInspection = null
        connection.shutdown()
    }
}

private data class AndroidReadyTtsRequest(
    val owner: AndroidTtsEngine,
    val request: ResolvedTtsRequest,
    val voice: android.speech.tts.Voice,
) : ReadyTtsEngineRequest
