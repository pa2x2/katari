package app.katari.translation.runtime.system

import app.katari.translation.api.engine.KnownTranslationEngine
import app.katari.translation.api.engine.TranslationEngineArtwork
import app.katari.translation.api.engine.TranslationEngineBuildAvailability
import app.katari.translation.api.engine.TranslationEngineDetails
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.engine.TranslationProviderId
import app.katari.translation.api.host.TranslationSetupDestination
import app.katari.translation.api.language.TranslationLanguageSupportInspection
import app.katari.translation.api.model.TranslationModelId
import app.katari.translation.api.model.TranslationModelOperationResult
import app.katari.translation.api.preparation.TranslationSystemSetupReason
import app.katari.translation.api.preparation.TranslationUnavailableReason
import app.katari.translation.api.provider.TranslationInvocationPolicy
import app.katari.translation.api.provider.TranslationProviderDisclosure
import app.katari.translation.api.provider.TranslationProviderPresentation
import app.katari.translation.api.request.ResolvedTranslationRoute
import app.katari.translation.runtime.R
import app.katari.translation.spi.engine.ReadyTranslationEngineRequest
import app.katari.translation.spi.engine.TranslationEngine
import app.katari.translation.spi.engine.TranslationEngineDeviceAvailability
import app.katari.translation.spi.engine.TranslationEngineExecution
import app.katari.translation.spi.engine.TranslationEnginePreparation
import app.katari.translation.spi.setup.TranslationEngineSetup
import app.katari.translation.spi.setup.TranslationSetupResult

internal class AndroidSystemTranslationEngine(
    private val platform: AndroidSystemTranslationPlatform,
) : TranslationEngine, TranslationEngineSetup {
    override val catalogEntry = KnownTranslationEngine(
        id = ENGINE_ID,
        providerId = PROVIDER_ID,
        providerName = "Android",
        engineName = "Android System Translation",
        buildAvailability = TranslationEngineBuildAvailability.Included,
        artwork = TranslationEngineArtwork.Bundled(R.drawable.ic_android_robot),
        details = TranslationEngineDetails(
            description = "Uses Android’s translation service on this device.",
            processingLocation = "Android translation service on this device.",
            privacyDescription = "Katari passes selected text to Android’s translation service. " +
                "Android controls model downloads and whether the configured service uses a network.",
            artworkAttribution = ANDROID_ROBOT_ATTRIBUTION,
            artworkAttributionUrl = ANDROID_BRAND_GUIDELINES_URL,
        ),
        documentationUrl = ANDROID_TRANSLATION_DOCUMENTATION_URL,
    )
    override val presentation = TranslationProviderPresentation(
        providerId = PROVIDER_ID,
        providerName = catalogEntry.providerName,
        engineName = catalogEntry.engineName,
        invocationPolicy = TranslationInvocationPolicy.Immediate,
    )
    override val maximumInputCodePoints: Int? = null
    override val engine: TranslationEngineId = ENGINE_ID
    override val supportsSetup = true

    override suspend fun inspectDevice(): TranslationEngineDeviceAvailability {
        return when (val inspection = platform.inspectDevice()) {
            AndroidSystemDeviceInspection.Available -> TranslationEngineDeviceAvailability.Available
            AndroidSystemDeviceInspection.UnsupportedOs ->
                TranslationEngineDeviceAvailability.UnsupportedOs(minimumApi = 31)
            AndroidSystemDeviceInspection.ServiceMissing -> TranslationEngineDeviceAvailability.ServiceMissing
            is AndroidSystemDeviceInspection.Failed ->
                TranslationEngineDeviceAvailability.Failed(inspection.reason)
        }
    }

    override suspend fun inspectLanguageSupport(): TranslationLanguageSupportInspection =
        platform.inspectLanguageSupport()

    override suspend fun prepare(route: ResolvedTranslationRoute): TranslationEnginePreparation {
        val ready = AndroidSystemReadyRequest(
            pair = AndroidSystemTranslationPair(route.sourceLanguage, route.targetLanguage),
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
        text: String,
    ): TranslationEngineExecution {
        val systemReady = ready.requireOwned()
        return when (
            val result = platform.translate(
                pair = systemReady.pair,
                text = text,
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
            AndroidSystemPlatformExecution.TranslatorRefused ->
                TranslationEngineExecution.PreparationChanged(
                    TranslationEnginePreparation.SystemSetupRequired(
                        TranslationSystemSetupReason.ProviderActionRequired(LANGUAGES_REFUSED_DESCRIPTION),
                    ),
                )
            is AndroidSystemPlatformExecution.Failed ->
                TranslationEngineExecution.Failed(result.reason)
        }
    }

    override suspend fun acknowledge(disclosure: TranslationProviderDisclosure) = Unit

    override suspend fun openSetup(): TranslationSetupResult {
        return when (val result = platform.openSettings()) {
            AndroidSystemPlatformSetup.Opened ->
                TranslationSetupResult.Opened(TranslationSetupDestination.External)
            AndroidSystemPlatformSetup.ServiceMissing -> TranslationSetupResult.ServiceMissing
            AndroidSystemPlatformSetup.SettingsUnavailable -> TranslationSetupResult.SettingsUnavailable
            is AndroidSystemPlatformSetup.Failed -> TranslationSetupResult.Failed(result.reason)
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
    ) : ReadyTranslationEngineRequest

    companion object {
        val ENGINE_ID = TranslationEngineId("android-system")
        val PROVIDER_ID = TranslationProviderId("android")
        private const val LANGUAGES_REFUSED_DESCRIPTION =
            "Android’s translation service didn’t accept these languages. " +
                "Download them again in Android’s translation settings."
        private const val ANDROID_ROBOT_ATTRIBUTION =
            "The Android robot is reproduced or modified from work created and shared by Google and used " +
                "according to terms described in the Creative Commons 3.0 Attribution License."
        private const val ANDROID_BRAND_GUIDELINES_URL =
            "https://developer.android.com/distribute/marketing-tools/brand-guidelines"
        private const val ANDROID_TRANSLATION_DOCUMENTATION_URL =
            "https://developer.android.com/reference/android/view/translation/TranslationManager"
    }
}
