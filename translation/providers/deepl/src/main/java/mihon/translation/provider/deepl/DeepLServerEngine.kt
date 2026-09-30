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
import mihon.translation.api.provider.TranslationInvocationPolicy
import mihon.translation.api.provider.TranslationProviderDisclosure
import mihon.translation.api.provider.TranslationProviderPresentation
import mihon.translation.api.provider.TranslationResultAttribution
import mihon.translation.api.request.ResolvedTranslationRoute
import mihon.translation.api.request.TranslationContext
import mihon.translation.provider.deepl.protocol.DeepLService
import mihon.translation.provider.deepl.protocol.toDeepLContext
import mihon.translation.provider.server.ServerConnectionSettings
import mihon.translation.provider.server.call.ServerCallException
import mihon.translation.provider.server.engine.ServerEngineReadiness
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
    settings: ServerConnectionSettings,
    private val serviceFactory: () -> DeepLService?,
    catalog: DeepLLanguageCatalog = DeepLLanguageCatalog(settings),
) : ContextualTranslationEngine {
    private val readiness = ServerEngineReadiness(
        engine = ENGINE_ID,
        settings = settings,
        disclosure = DISCLOSURE,
        configurationDescription = CONFIGURATION_DESCRIPTION,
        service = serviceFactory,
        languages = catalog::languages,
        // A language may have been added to the server since its languages were remembered.
        refreshedLanguages = catalog::refreshed,
    )
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

    override suspend fun inspectDevice(): TranslationEngineDeviceAvailability = readiness.inspectDevice()

    override suspend fun inspectLanguageSupport(): TranslationLanguageSupportInspection =
        readiness.inspectLanguageSupport()

    override suspend fun prepare(route: ResolvedTranslationRoute): TranslationEnginePreparation =
        readiness.prepare(route)

    override suspend fun revalidate(ready: ReadyTranslationEngineRequest): TranslationEnginePreparation {
        return prepare(readiness.owned(ready).route)
    }

    override suspend fun translate(
        ready: ReadyTranslationEngineRequest,
        segments: List<String>,
        context: TranslationContext,
    ): TranslationEngineBatchExecution {
        val owned = readiness.owned(ready)
        val service = serviceFactory()
            ?: return TranslationEngineBatchExecution.PreparationChanged(readiness.setupRequired())
        return try {
            TranslationEngineBatchExecution.Success(
                service.translate(segments, owned.sourceCode, owned.targetCode, context.toDeepLContext()),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: ServerCallException) {
            when (error.status) {
                401, 403 -> TranslationEngineBatchExecution.PreparationChanged(readiness.setupRequired())
                QUOTA_EXCEEDED_STATUS -> TranslationEngineBatchExecution.Failed(QUOTA_EXCEEDED_DESCRIPTION)
                else -> TranslationEngineBatchExecution.Failed(
                    error.localNetworkAccessDenial()?.message ?: FAILURE_DESCRIPTION,
                )
            }
        } catch (_: Exception) {
            TranslationEngineBatchExecution.Failed(FAILURE_DESCRIPTION)
        }
    }

    companion object {
        val ENGINE_ID = TranslationEngineId("deepl-compatible-server")
        val PROVIDER_ID = TranslationProviderId("deepl-compatible")
        const val PROVIDER_NAME = "DeepL API"
        const val ENGINE_NAME = "DeepL-compatible server"
        const val DOCUMENTATION_URL = "https://developers.deepl.com/docs"
        const val CONFIGURATION_DESCRIPTION = "Configure and test a DeepL-compatible server."
        private const val FAILURE_DESCRIPTION = "DeepL-compatible server did not complete the translation"
        private const val QUOTA_EXCEEDED_DESCRIPTION = "DeepL-compatible server translation quota is used up"

        /** DeepL's status for a used-up character quota. */
        private const val QUOTA_EXCEEDED_STATUS = 456

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
