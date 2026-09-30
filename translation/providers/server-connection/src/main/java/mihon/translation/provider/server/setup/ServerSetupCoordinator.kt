package mihon.translation.provider.server.setup

import eu.kanade.tachiyomi.network.localnetwork.LocalNetworkAccessDeniedException
import eu.kanade.tachiyomi.network.localnetwork.localNetworkAccessDenial
import kotlinx.coroutines.CancellationException
import mihon.translation.provider.server.ServerEndpointPolicy
import okhttp3.HttpUrl

sealed interface ServerSetupResult {
    data object InvalidEndpoint : ServerSetupResult

    data object Ready : ServerSetupResult

    data object ConnectionFailed : ServerSetupResult

    data class LocalNetworkAccessDenied(val message: String) : ServerSetupResult

    data object SaveFailed : ServerSetupResult
}

/**
 * Tests a server connection and saves it either way, remembering whether it passed.
 *
 * [verify] answers whether the server at an endpoint, reached with an API key, is usable.
 */
class ServerSetupCoordinator(
    private val verify: suspend (HttpUrl, String?) -> Boolean,
    private val saveConfiguration: (HttpUrl, String?, Boolean) -> Unit,
) {
    suspend fun saveAndTest(
        endpointText: String,
        apiKeyText: String,
    ): ServerSetupResult {
        val endpoint = ServerEndpointPolicy.validate(endpointText)
            ?: return ServerSetupResult.InvalidEndpoint
        val apiKey = apiKeyText.trim().takeIf(String::isNotEmpty)
        var localNetworkAccessDenial: LocalNetworkAccessDeniedException? = null
        val ready = try {
            verify(endpoint, apiKey)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            localNetworkAccessDenial = error.localNetworkAccessDenial()
            false
        }
        return try {
            saveConfiguration(endpoint, apiKey, ready)
            when {
                ready -> ServerSetupResult.Ready
                localNetworkAccessDenial != null -> ServerSetupResult.LocalNetworkAccessDenied(
                    localNetworkAccessDenial.message.orEmpty(),
                )
                else -> ServerSetupResult.ConnectionFailed
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            ServerSetupResult.SaveFailed
        }
    }
}
