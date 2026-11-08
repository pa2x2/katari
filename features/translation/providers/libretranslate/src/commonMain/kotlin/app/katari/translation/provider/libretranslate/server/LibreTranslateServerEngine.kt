package app.katari.translation.provider.libretranslate.server

import app.katari.translation.api.engine.KnownTranslationEngine
import app.katari.translation.api.engine.TranslationEngineArtwork
import app.katari.translation.api.engine.TranslationEngineBuildAvailability
import app.katari.translation.api.engine.TranslationEngineDetails
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.engine.TranslationProviderId
import app.katari.translation.api.language.TranslationLanguageSupportInspection
import app.katari.translation.api.provider.TranslationInvocationPolicy
import app.katari.translation.api.provider.TranslationProviderDisclosure
import app.katari.translation.api.provider.TranslationProviderPresentation
import app.katari.translation.api.provider.TranslationResultAttribution
import app.katari.translation.api.request.ResolvedTranslationRoute
import app.katari.translation.provider.libretranslate.protocol.LibreTranslateLanguageResolver
import app.katari.translation.provider.libretranslate.protocol.LibreTranslateService
import app.katari.translation.provider.libretranslate.resources.Res
import app.katari.translation.provider.libretranslate.resources.ic_libretranslate_server
import app.katari.translation.provider.server.ServerConnectionSettings
import app.katari.translation.provider.server.call.ServerCallException
import app.katari.translation.provider.server.engine.ServerEngineReadiness
import app.katari.translation.spi.engine.ReadyTranslationEngineRequest
import app.katari.translation.spi.engine.TranslationEngine
import app.katari.translation.spi.engine.TranslationEngineDeviceAvailability
import app.katari.translation.spi.engine.TranslationEngineExecution
import app.katari.translation.spi.engine.TranslationEnginePreparation
import eu.kanade.tachiyomi.network.localnetwork.localNetworkAccessDenial
import kotlinx.coroutines.CancellationException

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
        artwork = TranslationEngineArtwork.Glyph(Res.drawable.ic_libretranslate_server),
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
