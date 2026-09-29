package mihon.translation.provider.libretranslate.server.setup

import eu.kanade.tachiyomi.network.localnetwork.LocalNetworkAccessDeniedException
import eu.kanade.tachiyomi.network.localnetwork.localNetworkAccessDenial
import kotlinx.coroutines.CancellationException
import mihon.translation.provider.libretranslate.protocol.LibreTranslateService
import mihon.translation.provider.libretranslate.server.LibreTranslateServerConfiguration
import okhttp3.HttpUrl

internal sealed interface LibreTranslateServerSetupResult {
    data object InvalidEndpoint : LibreTranslateServerSetupResult

    data object Ready : LibreTranslateServerSetupResult

    data object ConnectionFailed : LibreTranslateServerSetupResult

    data class LocalNetworkAccessDenied(val message: String) : LibreTranslateServerSetupResult

    data object SaveFailed : LibreTranslateServerSetupResult
}

internal class LibreTranslateServerSetupCoordinator(
    private val serviceFactory: (HttpUrl, String?) -> LibreTranslateService,
    private val saveConfiguration: (HttpUrl, String?, Boolean) -> Unit,
) {
    suspend fun saveAndTest(
        endpointText: String,
        apiKeyText: String,
    ): LibreTranslateServerSetupResult {
        val endpoint = LibreTranslateServerConfiguration.validateEndpoint(endpointText)
            ?: return LibreTranslateServerSetupResult.InvalidEndpoint
        val apiKey = apiKeyText.trim().takeIf(String::isNotEmpty)
        var localNetworkAccessDenial: LocalNetworkAccessDeniedException? = null
        val ready = try {
            serviceFactory(endpoint, apiKey).languages().isNotEmpty()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            localNetworkAccessDenial = error.localNetworkAccessDenial()
            false
        }
        return try {
            saveConfiguration(endpoint, apiKey, ready)
            when {
                ready -> LibreTranslateServerSetupResult.Ready
                localNetworkAccessDenial != null -> LibreTranslateServerSetupResult.LocalNetworkAccessDenied(
                    localNetworkAccessDenial.message.orEmpty(),
                )
                else -> LibreTranslateServerSetupResult.ConnectionFailed
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            LibreTranslateServerSetupResult.SaveFailed
        }
    }
}
