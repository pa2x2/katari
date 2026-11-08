package app.katari.translation.provider.server.engine

import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.language.TranslationLanguageSupportInspection
import app.katari.translation.api.preparation.TranslationSystemSetupReason
import app.katari.translation.api.preparation.TranslationUnavailableReason
import app.katari.translation.api.provider.TranslationProviderDisclosure
import app.katari.translation.api.request.ResolvedTranslationRoute
import app.katari.translation.provider.server.ServerConnectionSettings
import app.katari.translation.provider.server.call.ServerCallException
import app.katari.translation.spi.engine.ReadyTranslationEngineRequest
import app.katari.translation.spi.engine.TranslationEngineDeviceAvailability
import app.katari.translation.spi.engine.TranslationEnginePreparation
import kotlinx.coroutines.CancellationException

/**
 * What an engine that translates through a server of the user's own checks before it translates: that a tested
 * connection is configured, that the server answers with its languages, that the user accepted the [disclosure], and
 * that the server translates between the languages asked for.
 *
 * @param service the provider's service for the configured connection, or null while none is configured.
 * @param languages asks [service] for the languages of the server.
 * @param refreshedLanguages the languages the server has now, or null when the ones given are as recent as can be
 * asked for; used when they lack a language, which the server may have gained since.
 */
class ServerEngineReadiness<S : Any, L : ServerLanguages>(
    private val engine: TranslationEngineId,
    private val settings: ServerConnectionSettings,
    private val disclosure: TranslationProviderDisclosure,
    private val configurationDescription: String,
    private val service: () -> S?,
    private val languages: suspend (S) -> L,
    private val refreshedLanguages: suspend (S, L) -> L? = { _, _ -> null },
) {
    suspend fun inspectDevice(): TranslationEngineDeviceAvailability {
        val service = verifiedService()
            ?: return TranslationEngineDeviceAvailability.ConfigurationRequired(configurationDescription)
        return try {
            if (languages(service).isEmpty) {
                TranslationEngineDeviceAvailability.ConfigurationRequired(configurationDescription)
            } else {
                TranslationEngineDeviceAvailability.Available
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TranslationEngineDeviceAvailability.Unavailable(UNREACHABLE_DESCRIPTION)
        }
    }

    suspend fun inspectLanguageSupport(): TranslationLanguageSupportInspection {
        val service = verifiedService()
            ?: return TranslationLanguageSupportInspection.Unavailable(configurationDescription)
        return try {
            languages(service).languageSupport()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TranslationLanguageSupportInspection.Unavailable("Configured server languages are unavailable")
        }
    }

    suspend fun prepare(route: ResolvedTranslationRoute): TranslationEnginePreparation {
        val service = verifiedService() ?: return setupRequired()
        return try {
            val languages = languages(service)
            if (!settings.disclosureAccepted) {
                return TranslationEnginePreparation.ProviderDisclosureRequired(disclosure)
            }
            val codes = languages.codes(route)
                ?: refreshedLanguages(service, languages)?.codes(route)
                ?: return TranslationEnginePreparation.Unavailable(
                    TranslationUnavailableReason.UnsupportedLanguagePair(route.sourceLanguage, route.targetLanguage),
                )
            TranslationEnginePreparation.Ready(ServerReadyRequest(route, codes.source, codes.target))
        } catch (error: CancellationException) {
            throw error
        } catch (error: ServerCallException) {
            if (error.status in UNAUTHORIZED_STATUSES) setupRequired() else unreachable()
        } catch (_: Exception) {
            unreachable()
        }
    }

    /** What [ready] was prepared for, which must have been prepared by this engine. */
    fun owned(ready: ReadyTranslationEngineRequest): ServerReadyRequest {
        require(ready is ServerReadyRequest && ready.route.engine == engine) {
            "Ready Translation request was not created by ${engine.value}"
        }
        return ready
    }

    fun setupRequired() = TranslationEnginePreparation.SystemSetupRequired(
        TranslationSystemSetupReason.ProviderActionRequired(configurationDescription),
    )

    private fun verifiedService(): S? = if (settings.isInitiallyVerified) service() else null

    private fun unreachable() = TranslationEnginePreparation.Unavailable(
        TranslationUnavailableReason.EngineUnavailable(engine, UNREACHABLE_DESCRIPTION),
    )

    private companion object {
        const val UNREACHABLE_DESCRIPTION = "Configured server is unreachable"
        val UNAUTHORIZED_STATUSES = setOf(401, 403)
    }
}

/** A [route] the server is ready to translate, with the codes it calls the route's languages by. */
data class ServerReadyRequest(
    val route: ResolvedTranslationRoute,
    val sourceCode: String,
    val targetCode: String,
) : ReadyTranslationEngineRequest
