package mihon.translation.provider.libretranslate.server

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
import mihon.translation.provider.libretranslate.R
import mihon.translation.provider.libretranslate.protocol.LibreTranslateLanguageResolver
import mihon.translation.provider.libretranslate.protocol.LibreTranslateService
import mihon.translation.provider.server.ServerConnectionSettings
import mihon.translation.provider.server.call.ServerCallException
import mihon.translation.provider.server.engine.ServerEngineReadiness
import mihon.translation.spi.engine.ReadyTranslationEngineRequest
import mihon.translation.spi.engine.TranslationEngine
import mihon.translation.spi.engine.TranslationEngineDeviceAvailability
import mihon.translation.spi.engine.TranslationEngineExecution
import mihon.translation.spi.engine.TranslationEnginePreparation

internal class LibreTranslateServerEngine(
    settings: ServerConnectionSettings,
    private val serviceFactory: () -> LibreTranslateService?,
) : TranslationEngine {
    private val readiness = ServerEngineReadiness(
        engine = ENGINE_ID,
        settings = settings,
        disclosure = DISCLOSURE,
        configurationDescription = CONFIGURATION_DESCRIPTION,
        service = serviceFactory,
        languages = { service -> LibreTranslateLanguageResolver(service.languages()) },
    )
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
        usesNetwork = true,
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

    override suspend fun inspectDevice(): TranslationEngineDeviceAvailability = readiness.inspectDevice()

    override suspend fun inspectLanguageSupport(): TranslationLanguageSupportInspection =
        readiness.inspectLanguageSupport()

    override suspend fun prepare(route: ResolvedTranslationRoute): TranslationEnginePreparation =
        readiness.prepare(route)

    override suspend fun revalidate(ready: ReadyTranslationEngineRequest): TranslationEnginePreparation {
        return prepare(readiness.owned(ready).route)
    }

    override suspend fun translate(ready: ReadyTranslationEngineRequest, text: String): TranslationEngineExecution {
        val owned = readiness.owned(ready)
        val service = serviceFactory()
            ?: return TranslationEngineExecution.PreparationChanged(readiness.setupRequired())
        return try {
            TranslationEngineExecution.Success(
                service.translate(text, owned.sourceCode, owned.targetCode),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: ServerCallException) {
            if (error.isRejection) {
                TranslationEngineExecution.PreparationChanged(readiness.setupRequired())
            } else {
                TranslationEngineExecution.Failed(
                    error.localNetworkAccessDenial()?.message
                        ?: "LibreTranslate Server did not complete the translation",
                )
            }
        } catch (_: Exception) {
            TranslationEngineExecution.Failed("LibreTranslate Server did not complete the translation")
        }
    }

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
