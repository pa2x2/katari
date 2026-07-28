package mihon.translation.runtime.system

import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.ResolvedTranslationRequest
import mihon.translation.api.TranslationEngineBuildAvailability
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationInvocationPolicy
import mihon.translation.api.TranslationModelId
import mihon.translation.api.TranslationModelOperationResult
import mihon.translation.api.TranslationProviderId
import mihon.translation.api.TranslationProviderPresentation
import mihon.translation.api.TranslationSystemSetupReason
import mihon.translation.api.TranslationUnavailableReason
import mihon.translation.spi.ReadyTranslationEngineRequest
import mihon.translation.spi.TranslationEngine
import mihon.translation.spi.TranslationEngineExecution
import mihon.translation.spi.TranslationEnginePreparation
import mihon.translation.spi.TranslationEngineSetup
import mihon.translation.spi.TranslationSystemSetupResult

internal class AndroidSystemTranslationEngine(
    private val platform: AndroidSystemTranslationPlatform,
) : TranslationEngine, TranslationEngineSetup {
    override val catalogEntry = KnownTranslationEngine(
        id = ENGINE_ID,
        providerId = PROVIDER_ID,
        providerName = "Android",
        engineName = "System on-device translation",
        buildAvailability = TranslationEngineBuildAvailability.Included,
    )
    override val presentation = TranslationProviderPresentation(
        providerId = PROVIDER_ID,
        providerName = catalogEntry.providerName,
        engineName = catalogEntry.engineName,
        invocationPolicy = TranslationInvocationPolicy.Immediate,
    )
    override val maximumInputCodePoints: Int? = null
    override val engine: TranslationEngineId = ENGINE_ID

    override suspend fun prepare(request: ResolvedTranslationRequest): TranslationEnginePreparation {
        val ready = AndroidSystemReadyRequest(
            pair = AndroidSystemTranslationPair(request.sourceLanguage, request.targetLanguage),
            text = request.text,
        )
        return platform.inspect(ready.pair).toPreparation(ready)
    }

    override suspend fun revalidate(
        ready: ReadyTranslationEngineRequest,
    ): TranslationEnginePreparation {
        val systemReady = ready.requireOwned()
        return platform.inspect(systemReady.pair).toPreparation(systemReady)
    }

    override suspend fun translate(
        ready: ReadyTranslationEngineRequest,
    ): TranslationEngineExecution {
        val systemReady = ready.requireOwned()
        return when (
            val result = platform.translate(
                pair = systemReady.pair,
                text = systemReady.text,
            )
        ) {
            is AndroidSystemPlatformExecution.Success ->
                TranslationEngineExecution.Success(result.translatedText)
            is AndroidSystemPlatformExecution.CapabilityChanged ->
                TranslationEngineExecution.PreparationChanged(
                    result.inspection.toPreparation(systemReady),
                )
            AndroidSystemPlatformExecution.ContextUnsupported ->
                TranslationEngineExecution.PreparationChanged(
                    TranslationEnginePreparation.Unavailable(
                        TranslationUnavailableReason.UnsupportedLanguagePair(
                            systemReady.pair.source,
                            systemReady.pair.target,
                        ),
                    ),
                )
            is AndroidSystemPlatformExecution.Failed ->
                TranslationEngineExecution.Failed(result.reason)
        }
    }

    override suspend fun acknowledge(disclosure: mihon.translation.api.TranslationProviderDisclosure) = Unit

    override suspend fun openSystemSetup(): TranslationSystemSetupResult {
        return when (val result = platform.openSettings()) {
            AndroidSystemPlatformSetup.Opened -> TranslationSystemSetupResult.Opened
            AndroidSystemPlatformSetup.ServiceMissing -> TranslationSystemSetupResult.ServiceMissing
            AndroidSystemPlatformSetup.SettingsUnavailable -> TranslationSystemSetupResult.SettingsUnavailable
            is AndroidSystemPlatformSetup.Failed -> TranslationSystemSetupResult.Failed(result.reason)
        }
    }

    override suspend fun downloadModels(
        models: Set<TranslationModelId>,
        allowMeteredNetwork: Boolean,
    ): TranslationModelOperationResult {
        return TranslationModelOperationResult.Failed(
            "Android system translation models are managed by system settings",
        )
    }

    private fun ReadyTranslationEngineRequest.requireOwned(): AndroidSystemReadyRequest {
        require(this is AndroidSystemReadyRequest) {
            "Ready Translation request was not created by the Android system engine"
        }
        return this
    }

    private fun AndroidSystemTranslationInspection.toPreparation(
        ready: AndroidSystemReadyRequest,
    ): TranslationEnginePreparation {
        return when (this) {
            AndroidSystemTranslationInspection.UnsupportedOs ->
                TranslationEnginePreparation.Unavailable(TranslationUnavailableReason.UnsupportedOs(31))
            AndroidSystemTranslationInspection.ServiceMissing ->
                TranslationEnginePreparation.Unavailable(TranslationUnavailableReason.ServiceMissing)
            AndroidSystemTranslationInspection.UnsupportedPair ->
                TranslationEnginePreparation.Unavailable(
                    TranslationUnavailableReason.UnsupportedLanguagePair(
                        ready.pair.source,
                        ready.pair.target,
                    ),
                )
            is AndroidSystemTranslationInspection.Failed ->
                TranslationEnginePreparation.Unavailable(
                    TranslationUnavailableReason.EngineUnavailable(ENGINE_ID, reason),
                )
            is AndroidSystemTranslationInspection.Capability -> when (state) {
                AndroidSystemCapabilityState.OnDevice -> TranslationEnginePreparation.Ready(ready)
                AndroidSystemCapabilityState.AvailableToDownload ->
                    TranslationEnginePreparation.SystemSetupRequired(
                        TranslationSystemSetupReason.LanguageModelsRequired,
                    )
                AndroidSystemCapabilityState.Downloading ->
                    TranslationEnginePreparation.SetupInProgress()
                AndroidSystemCapabilityState.Unavailable ->
                    TranslationEnginePreparation.Unavailable(
                        TranslationUnavailableReason.UnsupportedLanguagePair(
                            ready.pair.source,
                            ready.pair.target,
                        ),
                    )
            }
        }
    }

    private data class AndroidSystemReadyRequest(
        val pair: AndroidSystemTranslationPair,
        val text: String,
    ) : ReadyTranslationEngineRequest

    companion object {
        val ENGINE_ID = TranslationEngineId("android-system")
        val PROVIDER_ID = TranslationProviderId("android")
    }
}
