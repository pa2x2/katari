package mihon.translation.provider.deepl

import eu.kanade.tachiyomi.network.localnetwork.localNetworkAccessDenial
import kotlinx.coroutines.CancellationException
import mihon.translation.api.engine.KnownTranslationEngine
import mihon.translation.api.engine.TranslationEngineArtwork
import mihon.translation.api.engine.TranslationEngineBuildAvailability
import mihon.translation.api.engine.TranslationEngineDetails
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationProviderId
import mihon.translation.api.language.TranslationLanguageSupportInspection
import mihon.translation.api.preparation.TranslationSystemSetupReason
import mihon.translation.api.preparation.TranslationUnavailableReason
import mihon.translation.api.provider.TranslationInvocationPolicy
import mihon.translation.api.provider.TranslationProviderDisclosure
import mihon.translation.api.provider.TranslationProviderPresentation
import mihon.translation.api.provider.TranslationResultAttribution
import mihon.translation.api.request.ResolvedTranslationRoute
import mihon.translation.api.request.TranslationContext
import mihon.translation.provider.deepl.protocol.DeepLException
import mihon.translation.provider.deepl.protocol.DeepLFailureKind
import mihon.translation.provider.deepl.protocol.DeepLLanguageResolver
import mihon.translation.provider.deepl.protocol.DeepLService
import mihon.translation.provider.deepl.protocol.toDeepLContext
import mihon.translation.provider.server.ServerConnectionSettings
import mihon.translation.spi.engine.ContextualTranslationEngine
import mihon.translation.spi.engine.ReadyTranslationEngineRequest
import mihon.translation.spi.engine.TranslationContextElement
import mihon.translation.spi.engine.TranslationEngineBatchExecution
import mihon.translation.spi.engine.TranslationEngineDeviceAvailability
import mihon.translation.spi.engine.TranslationEnginePreparation

/**
 * Translates through DeepL, or through a server of the user's own that speaks the DeepL API, passing along the
 * context of each text so the server can translate in light of it.
 */
internal class DeepLServerEngine(
    private val settings: ServerConnectionSettings,
    private val serviceFactory: () -> DeepLService?,
    private val catalog: DeepLLanguageCatalog = DeepLLanguageCatalog(settings),
) : ContextualTranslationEngine {
    override val catalogEntry = KnownTranslationEngine(
        id = ENGINE_ID,
        providerId = PROVIDER_ID,
        providerName = PROVIDER_NAME,
        engineName = ENGINE_NAME,
        buildAvailability = TranslationEngineBuildAvailability.Included,
        artwork = TranslationEngineArtwork.Bundled(R.drawable.ic_deepl_compatible_server),
        details = TranslationEngineDetails(
            description = "Translation in context through DeepL or a server of your own that speaks the DeepL API.",
            processingLocation = "The configured DeepL-compatible server.",
            privacyDescription = "Katari sends selected text to the configured server, along with the title and " +
                "description of what you are reading and the text that comes before. The server operator’s " +
                "privacy and retention policies apply.",
        ),
        documentationUrl = DOCUMENTATION_URL,
        usesNetwork = true,
    )
    override val presentation = TranslationProviderPresentation(
        providerId = PROVIDER_ID,
        providerName = PROVIDER_NAME,
        engineName = ENGINE_NAME,
        invocationPolicy = TranslationInvocationPolicy.Immediate,
        disclosure = DISCLOSURE,
        resultAttribution = TranslationResultAttribution(ENGINE_NAME),
        documentationUrl = DOCUMENTATION_URL,
    )
    override val maximumInputCodePoints: Int? = null
    override val contextSupport = setOf(TranslationContextElement.Work, TranslationContextElement.PrecedingText)
    override val maximumBatchSegments = MAXIMUM_BATCH_SEGMENTS

    override suspend fun inspectDevice(): TranslationEngineDeviceAvailability {
        if (!settings.isInitiallyVerified) {
            return TranslationEngineDeviceAvailability.ConfigurationRequired(CONFIGURATION_DESCRIPTION)
        }
        val service = serviceFactory()
            ?: return TranslationEngineDeviceAvailability.ConfigurationRequired(CONFIGURATION_DESCRIPTION)
        return try {
            val languages = catalog.languages(service)
            if (languages.sources.isEmpty() || languages.targets.isEmpty()) {
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
            DeepLLanguageResolver(catalog.languages(service)).languageSupport()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TranslationLanguageSupportInspection.Unavailable("Configured server languages are unavailable")
        }
    }

    override suspend fun prepare(route: ResolvedTranslationRoute): TranslationEnginePreparation {
        if (!settings.isInitiallyVerified) return setupRequired()
        val service = serviceFactory() ?: return setupRequired()
        val languages = try {
            catalog.languages(service)
        } catch (error: CancellationException) {
            throw error
        } catch (error: DeepLException) {
            if (error.kind == DeepLFailureKind.Unauthorized) return setupRequired()
            return unreachable()
        } catch (_: Exception) {
            return unreachable()
        }
        if (!settings.disclosureAccepted) {
            return TranslationEnginePreparation.ProviderDisclosureRequired(DISCLOSURE)
        }
        val resolver = DeepLLanguageResolver(languages)
        val source = resolver.source(route.sourceLanguage)
        val target = resolver.target(route.targetLanguage)
        if (source == null || target == null) {
            return TranslationEnginePreparation.Unavailable(
                TranslationUnavailableReason.UnsupportedLanguagePair(route.sourceLanguage, route.targetLanguage),
            )
        }
        return TranslationEnginePreparation.Ready(ReadyRequest(route, source, target))
    }

    override suspend fun revalidate(ready: ReadyTranslationEngineRequest): TranslationEnginePreparation {
        return prepare(ready.requireOwned().route)
    }

    override suspend fun translate(
        ready: ReadyTranslationEngineRequest,
        segments: List<String>,
        context: TranslationContext,
    ): TranslationEngineBatchExecution {
        val owned = ready.requireOwned()
        val service = serviceFactory()
            ?: return TranslationEngineBatchExecution.PreparationChanged(setupRequired())
        return try {
            TranslationEngineBatchExecution.Success(
                service.translate(segments, owned.sourceCode, owned.targetCode, context.toDeepLContext()),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: DeepLException) {
            when (error.kind) {
                DeepLFailureKind.Unauthorized -> TranslationEngineBatchExecution.PreparationChanged(setupRequired())
                DeepLFailureKind.QuotaExceeded -> TranslationEngineBatchExecution.Failed(error.message)
                else -> TranslationEngineBatchExecution.Failed(
                    error.localNetworkAccessDenial()?.message ?: FAILURE_DESCRIPTION,
                )
            }
        } catch (_: Exception) {
            TranslationEngineBatchExecution.Failed(FAILURE_DESCRIPTION)
        }
    }

    private fun setupRequired() = TranslationEnginePreparation.SystemSetupRequired(
        TranslationSystemSetupReason.ProviderActionRequired(CONFIGURATION_DESCRIPTION),
    )

    private fun unreachable() = TranslationEnginePreparation.Unavailable(
        TranslationUnavailableReason.EngineUnavailable(ENGINE_ID, "Configured server is unreachable"),
    )

    private fun ReadyTranslationEngineRequest.requireOwned(): ReadyRequest {
        require(this is ReadyRequest) {
            "Ready Translation request was not created by the DeepL-compatible server engine"
        }
        return this
    }

    private data class ReadyRequest(
        val route: ResolvedTranslationRoute,
        val sourceCode: String,
        val targetCode: String,
    ) : ReadyTranslationEngineRequest

    companion object {
        val ENGINE_ID = TranslationEngineId("deepl-compatible-server")
        val PROVIDER_ID = TranslationProviderId("deepl-compatible")
        const val PROVIDER_NAME = "DeepL API"
        const val ENGINE_NAME = "DeepL-compatible server"
        const val DOCUMENTATION_URL = "https://developers.deepl.com/docs"
        const val CONFIGURATION_DESCRIPTION = "Configure and test a DeepL-compatible server."
        private const val FAILURE_DESCRIPTION = "DeepL-compatible server did not complete the translation"

        /** Well within what one DeepL API request may carry, and more than a page has speech bubbles. */
        private const val MAXIMUM_BATCH_SEGMENTS = 50
        val DISCLOSURE = TranslationProviderDisclosure(
            title = "Use a DeepL-compatible server",
            message = "Katari sends selected text to the configured server, along with the title and description " +
                "of what you are reading and the text that comes before it. " +
                "The server operator’s privacy and retention policies apply.",
            confirmationLabel = "Allow server translation",
            documentationUrl = DOCUMENTATION_URL,
        )
    }
}
