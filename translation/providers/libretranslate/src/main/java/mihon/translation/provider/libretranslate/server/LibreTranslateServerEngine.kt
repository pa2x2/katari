package mihon.translation.provider.libretranslate.server

import kotlinx.coroutines.CancellationException
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.ResolvedTranslationRequest
import mihon.translation.api.TranslationEngineArtwork
import mihon.translation.api.TranslationEngineBuildAvailability
import mihon.translation.api.TranslationEngineDetails
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationInvocationPolicy
import mihon.translation.api.TranslationLanguageSupportInspection
import mihon.translation.api.TranslationProviderDisclosure
import mihon.translation.api.TranslationProviderId
import mihon.translation.api.TranslationProviderPresentation
import mihon.translation.api.TranslationResultAttribution
import mihon.translation.api.TranslationSystemSetupReason
import mihon.translation.api.TranslationUnavailableReason
import mihon.translation.provider.libretranslate.R
import mihon.translation.provider.libretranslate.protocol.LibreTranslateException
import mihon.translation.provider.libretranslate.protocol.LibreTranslateFailureKind
import mihon.translation.provider.libretranslate.protocol.LibreTranslateLanguageResolver
import mihon.translation.provider.libretranslate.protocol.LibreTranslateService
import mihon.translation.spi.ReadyTranslationEngineRequest
import mihon.translation.spi.TranslationEngine
import mihon.translation.spi.TranslationEngineDeviceAvailability
import mihon.translation.spi.TranslationEngineExecution
import mihon.translation.spi.TranslationEnginePreparation

internal class LibreTranslateServerEngine(
    private val settings: LibreTranslateServerSettings,
    private val serviceFactory: () -> LibreTranslateService?,
) : TranslationEngine {
    override val catalogEntry = KnownTranslationEngine(
        id = ENGINE_ID,
        providerId = PROVIDER_ID,
        providerName = PROVIDER_NAME,
        engineName = ENGINE_NAME,
        buildAvailability = TranslationEngineBuildAvailability.Included,
        artwork = TranslationEngineArtwork.Bundled(R.drawable.ic_libretranslate_server),
        details = TranslationEngineDetails(
            description = "Translation through a server you choose and configure.",
            processingLocation = "The configured LibreTranslate-compatible server.",
            privacyDescription = "Katari sends selected text to the configured server. The server operator’s " +
                "privacy and retention policies apply.",
        ),
        documentationUrl = DOCUMENTATION_URL,
    )
    override val presentation = TranslationProviderPresentation(
        providerId = PROVIDER_ID,
        providerName = PROVIDER_NAME,
        engineName = ENGINE_NAME,
        invocationPolicy = TranslationInvocationPolicy.Immediate,
        disclosure = DISCLOSURE,
        resultAttribution = TranslationResultAttribution(PROVIDER_NAME),
        documentationUrl = DOCUMENTATION_URL,
    )
    override val maximumInputCodePoints: Int? = null

    override suspend fun inspectDevice(): TranslationEngineDeviceAvailability {
        if (!settings.isInitiallyVerified) {
            return TranslationEngineDeviceAvailability.ConfigurationRequired(CONFIGURATION_DESCRIPTION)
        }
        val service = serviceFactory()
            ?: return TranslationEngineDeviceAvailability.ConfigurationRequired(CONFIGURATION_DESCRIPTION)
        return try {
            if (service.languages().isEmpty()) {
                TranslationEngineDeviceAvailability.ConfigurationRequired(CONFIGURATION_DESCRIPTION)
            } else {
                TranslationEngineDeviceAvailability.Available
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TranslationEngineDeviceAvailability.Unavailable("Configured server is unreachable")
        }
    }

    override suspend fun inspectLanguageSupport(): TranslationLanguageSupportInspection {
        if (!settings.isInitiallyVerified) {
            return TranslationLanguageSupportInspection.Unavailable(CONFIGURATION_DESCRIPTION)
        }
        val service = serviceFactory()
            ?: return TranslationLanguageSupportInspection.Unavailable(CONFIGURATION_DESCRIPTION)
        return try {
            LibreTranslateLanguageResolver(service.languages()).languageSupport()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TranslationLanguageSupportInspection.Unavailable(
                "Configured server languages are unavailable",
            )
        }
    }

    override suspend fun prepare(request: ResolvedTranslationRequest): TranslationEnginePreparation {
        if (!settings.isInitiallyVerified) return setupRequired()
        val service = serviceFactory() ?: return setupRequired()
        val languages = try {
            service.languages()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return TranslationEnginePreparation.Unavailable(
                TranslationUnavailableReason.EngineUnavailable(ENGINE_ID, "Configured server is unreachable"),
            )
        }
        if (!settings.disclosureAccepted) {
            return TranslationEnginePreparation.ProviderDisclosureRequired(DISCLOSURE)
        }
        val resolver = LibreTranslateLanguageResolver(languages)
        val source = resolver.resolve(request.sourceLanguage)
            ?: return TranslationEnginePreparation.Unavailable(
                TranslationUnavailableReason.UnsupportedLanguagePair(
                    request.sourceLanguage,
                    request.targetLanguage,
                ),
            )
        val target = resolver.resolve(request.targetLanguage)
            ?: return TranslationEnginePreparation.Unavailable(
                TranslationUnavailableReason.UnsupportedLanguagePair(
                    request.sourceLanguage,
                    request.targetLanguage,
                ),
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
            ReadyRequest(request, source.code, target.code),
        )
    }

    override suspend fun revalidate(ready: ReadyTranslationEngineRequest): TranslationEnginePreparation {
        return prepare(ready.requireOwned().request)
    }

    override suspend fun translate(ready: ReadyTranslationEngineRequest): TranslationEngineExecution {
        val owned = ready.requireOwned()
        val service = serviceFactory()
            ?: return TranslationEngineExecution.PreparationChanged(setupRequired())
        return try {
            TranslationEngineExecution.Success(
                service.translate(owned.request.text, owned.sourceCode, owned.targetCode),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: LibreTranslateException) {
            if (error.kind == LibreTranslateFailureKind.Rejected) {
                TranslationEngineExecution.PreparationChanged(setupRequired())
            } else {
                TranslationEngineExecution.Failed("LibreTranslate Server did not complete the translation")
            }
        } catch (_: Exception) {
            TranslationEngineExecution.Failed("LibreTranslate Server did not complete the translation")
        }
    }

    private fun setupRequired() = TranslationEnginePreparation.SystemSetupRequired(
        TranslationSystemSetupReason.ProviderActionRequired(CONFIGURATION_DESCRIPTION),
    )

    private fun ReadyTranslationEngineRequest.requireOwned(): ReadyRequest {
        require(this is ReadyRequest) {
            "Ready Translation request was not created by LibreTranslate Server"
        }
        return this
    }

    private data class ReadyRequest(
        val request: ResolvedTranslationRequest,
        val sourceCode: String,
        val targetCode: String,
    ) : ReadyTranslationEngineRequest

    companion object {
        val ENGINE_ID = TranslationEngineId("libretranslate-server")
        val PROVIDER_ID = TranslationProviderId("libretranslate")
        const val PROVIDER_NAME = "LibreTranslate"
        const val ENGINE_NAME = "LibreTranslate Server"
        const val DOCUMENTATION_URL = "https://docs.libretranslate.com/"
        const val CONFIGURATION_DESCRIPTION = "Configure and test a LibreTranslate server."
        val DISCLOSURE = TranslationProviderDisclosure(
            title = "Use LibreTranslate Server",
            message = "Katari sends selected text to the configured LibreTranslate server. " +
                "The server operator’s privacy and retention policies apply.",
            confirmationLabel = "Allow server translation",
            documentationUrl = DOCUMENTATION_URL,
        )
    }
}
