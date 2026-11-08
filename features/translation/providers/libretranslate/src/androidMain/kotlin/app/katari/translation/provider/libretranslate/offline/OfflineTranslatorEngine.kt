package app.katari.translation.provider.libretranslate.offline

import app.katari.translation.api.engine.KnownTranslationEngine
import app.katari.translation.api.engine.TranslationEngineArtwork
import app.katari.translation.api.engine.TranslationEngineBuildAvailability
import app.katari.translation.api.engine.TranslationEngineDetails
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.engine.TranslationProviderId
import app.katari.translation.api.language.TranslationLanguageSupportInspection
import app.katari.translation.api.preparation.TranslationSystemSetupReason
import app.katari.translation.api.preparation.TranslationUnavailableReason
import app.katari.translation.api.provider.TranslationInvocationPolicy
import app.katari.translation.api.provider.TranslationProviderDisclosure
import app.katari.translation.api.provider.TranslationProviderPresentation
import app.katari.translation.api.provider.TranslationResultAttribution
import app.katari.translation.api.request.ResolvedTranslationRoute
import app.katari.translation.provider.libretranslate.R
import app.katari.translation.provider.libretranslate.protocol.LibreTranslateLanguageResolver
import app.katari.translation.provider.libretranslate.protocol.LibreTranslateService
import app.katari.translation.provider.server.call.ServerCallException
import app.katari.translation.spi.engine.ReadyTranslationEngineRequest
import app.katari.translation.spi.engine.TranslationEngine
import app.katari.translation.spi.engine.TranslationEngineDeviceAvailability
import app.katari.translation.spi.engine.TranslationEngineExecution
import app.katari.translation.spi.engine.TranslationEnginePreparation
import kotlinx.coroutines.CancellationException
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
        artwork = TranslationEngineArtwork.InstalledApplication(
            packageName = OfflineTranslatorApplication.PACKAGE_NAME,
            fallbackResourceId = R.drawable.ic_offline_translator,
        ),
        details = TranslationEngineDetails(
            description = "Private on-device translation through the Offline Translator app.",
            processingLocation = "Offline Translator on this device, over localhost.",
            privacyDescription = "Katari sends selected text to the separate Offline Translator app over " +
                "127.0.0.1. Translation stays on this device when its HTTP API is bound to localhost.",
        ),
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

    override suspend fun inspectLanguageSupport(): TranslationLanguageSupportInspection {
        if (!application.isInstalled()) {
            return TranslationLanguageSupportInspection.Unavailable(
                "Offline Translator is not installed",
            )
        }
        return try {
            LibreTranslateLanguageResolver(service().languages()).languageSupport()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TranslationLanguageSupportInspection.Unavailable(
                "Offline Translator languages are unavailable",
            )
        }
    }

    override suspend fun prepare(route: ResolvedTranslationRoute): TranslationEnginePreparation {
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

        val codes = LibreTranslateLanguageResolver(languages).codes(route)
            ?: return TranslationEnginePreparation.Unavailable(
                TranslationUnavailableReason.UnsupportedLanguagePair(
                    route.sourceLanguage,
                    route.targetLanguage,
                ),
            )
        return TranslationEnginePreparation.Ready(
            OfflineTranslatorReadyRequest(
                route = route,
                sourceCode = codes.source,
                targetCode = codes.target,
            ),
        )
    }

    override suspend fun revalidate(
        ready: ReadyTranslationEngineRequest,
    ): TranslationEnginePreparation {
        val owned = ready.requireOwned()
        return prepare(owned.route)
    }

    override suspend fun translate(
        ready: ReadyTranslationEngineRequest,
        text: String,
    ): TranslationEngineExecution {
        val owned = ready.requireOwned()
        return try {
            TranslationEngineExecution.Success(
                service().translate(
                    text = text,
                    source = owned.sourceCode,
                    target = owned.targetCode,
                ),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: ServerCallException) {
            if (error.isRejection) {
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
        val route: ResolvedTranslationRoute,
        val sourceCode: String,
        val targetCode: String,
    ) : ReadyTranslationEngineRequest

    companion object {
        val ENGINE_ID = TranslationEngineId("offline-translator")
        val PROVIDER_ID = TranslationProviderId("offline-translator")
        const val PROVIDER_NAME = "Offline Translator"
        const val ENGINE_NAME = "Offline Translator"
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
