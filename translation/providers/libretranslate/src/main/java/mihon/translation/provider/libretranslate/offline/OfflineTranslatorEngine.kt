package mihon.translation.provider.libretranslate.offline

import kotlinx.coroutines.CancellationException
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.ResolvedTranslationRequest
import mihon.translation.api.TranslationEngineBuildAvailability
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationInvocationPolicy
import mihon.translation.api.TranslationProviderDisclosure
import mihon.translation.api.TranslationProviderId
import mihon.translation.api.TranslationProviderPresentation
import mihon.translation.api.TranslationResultAttribution
import mihon.translation.api.TranslationSystemSetupReason
import mihon.translation.api.TranslationUnavailableReason
import mihon.translation.provider.libretranslate.protocol.LibreTranslateException
import mihon.translation.provider.libretranslate.protocol.LibreTranslateFailureKind
import mihon.translation.provider.libretranslate.protocol.LibreTranslateLanguageResolver
import mihon.translation.provider.libretranslate.protocol.LibreTranslateService
import mihon.translation.spi.ReadyTranslationEngineRequest
import mihon.translation.spi.TranslationEngine
import mihon.translation.spi.TranslationEngineDeviceAvailability
import mihon.translation.spi.TranslationEngineExecution
import mihon.translation.spi.TranslationEnginePreparation
import okhttp3.HttpUrl

internal class OfflineTranslatorEngine(
    private val application: OfflineTranslatorApp,
    private val settings: OfflineTranslatorSettings,
    private val serviceFactory: (HttpUrl) -> LibreTranslateService,
) : TranslationEngine {
    override val catalogEntry = KnownTranslationEngine(
        id = ENGINE_ID,
        providerId = PROVIDER_ID,
        providerName = PROVIDER_NAME,
        engineName = ENGINE_NAME,
        buildAvailability = TranslationEngineBuildAvailability.Included,
        documentationUrl = OfflineTranslatorApplication.DOCUMENTATION_URL,
    )
    override val presentation = TranslationProviderPresentation(
        providerId = PROVIDER_ID,
        providerName = PROVIDER_NAME,
        engineName = ENGINE_NAME,
        invocationPolicy = TranslationInvocationPolicy.Immediate,
        disclosure = DISCLOSURE,
        resultAttribution = TranslationResultAttribution(PROVIDER_NAME),
        documentationUrl = OfflineTranslatorApplication.DOCUMENTATION_URL,
    )
    override val maximumInputCodePoints: Int? = null

    override suspend fun inspectDevice(): TranslationEngineDeviceAvailability {
        if (!application.isInstalled()) return TranslationEngineDeviceAvailability.NotInstalled
        return try {
            if (service().languages().isEmpty()) {
                TranslationEngineDeviceAvailability.ConfigurationRequired(SETUP_DESCRIPTION)
            } else {
                TranslationEngineDeviceAvailability.Available
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TranslationEngineDeviceAvailability.ConfigurationRequired(SETUP_DESCRIPTION)
        }
    }

    override suspend fun prepare(request: ResolvedTranslationRequest): TranslationEnginePreparation {
        if (!application.isInstalled()) {
            return TranslationEnginePreparation.Unavailable(
                TranslationUnavailableReason.EngineUnavailable(
                    ENGINE_ID,
                    "Offline Translator is not installed",
                ),
            )
        }
        val languages = try {
            service().languages()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return setupRequired()
        }
        if (languages.isEmpty()) return setupRequired()
        if (!settings.disclosureAccepted) {
            return TranslationEnginePreparation.ProviderDisclosureRequired(DISCLOSURE)
        }

        val resolver = LibreTranslateLanguageResolver(languages)
        val source = resolver.resolve(request.sourceLanguage)
            ?: return TranslationEnginePreparation.Unavailable(
                TranslationUnavailableReason.UnsupportedLanguage(request.sourceLanguage),
            )
        val target = resolver.resolve(request.targetLanguage)
            ?: return TranslationEnginePreparation.Unavailable(
                TranslationUnavailableReason.UnsupportedLanguage(request.targetLanguage),
            )
        if (!resolver.supportsTarget(source, target)) {
            return TranslationEnginePreparation.Unavailable(
                TranslationUnavailableReason.UnsupportedLanguagePair(
                    request.sourceLanguage,
                    request.targetLanguage,
                ),
            )
        }
        return TranslationEnginePreparation.Ready(
            OfflineTranslatorReadyRequest(
                request = request,
                sourceCode = source.code,
                targetCode = target.code,
            ),
        )
    }

    override suspend fun revalidate(
        ready: ReadyTranslationEngineRequest,
    ): TranslationEnginePreparation {
        val owned = ready.requireOwned()
        return prepare(owned.request)
    }

    override suspend fun translate(
        ready: ReadyTranslationEngineRequest,
    ): TranslationEngineExecution {
        val owned = ready.requireOwned()
        return try {
            TranslationEngineExecution.Success(
                service().translate(
                    text = owned.request.text,
                    source = owned.sourceCode,
                    target = owned.targetCode,
                ),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: LibreTranslateException) {
            if (error.kind == LibreTranslateFailureKind.Rejected) {
                TranslationEngineExecution.PreparationChanged(setupRequired())
            } else {
                TranslationEngineExecution.Failed("Offline Translator did not complete the translation")
            }
        } catch (_: Exception) {
            TranslationEngineExecution.Failed("Offline Translator did not complete the translation")
        }
    }

    private fun service(): LibreTranslateService = serviceFactory(settings.endpoint())

    private fun ReadyTranslationEngineRequest.requireOwned(): OfflineTranslatorReadyRequest {
        require(this is OfflineTranslatorReadyRequest) {
            "Ready Translation request was not created by Offline Translator"
        }
        return this
    }

    private fun setupRequired(): TranslationEnginePreparation.SystemSetupRequired {
        return TranslationEnginePreparation.SystemSetupRequired(
            TranslationSystemSetupReason.ProviderActionRequired(SETUP_DESCRIPTION),
        )
    }

    private data class OfflineTranslatorReadyRequest(
        val request: ResolvedTranslationRequest,
        val sourceCode: String,
        val targetCode: String,
    ) : ReadyTranslationEngineRequest

    companion object {
        val ENGINE_ID = TranslationEngineId("offline-translator")
        val PROVIDER_ID = TranslationProviderId("offline-translator")
        const val PROVIDER_NAME = "Offline Translator"
        const val ENGINE_NAME = "Offline on-device translation"
        const val SETUP_DESCRIPTION =
            "Enable the HTTP API in Offline Translator, download the required language models, and verify the port."
        val DISCLOSURE = TranslationProviderDisclosure(
            title = "Use Offline Translator",
            message = "Katari sends selected text to Offline Translator over 127.0.0.1. " +
                "Translation remains on this device. Keep the provider HTTP API bound to localhost.",
            confirmationLabel = "Allow on-device translation",
            documentationUrl = OfflineTranslatorApplication.DOCUMENTATION_URL,
        )
    }
}
