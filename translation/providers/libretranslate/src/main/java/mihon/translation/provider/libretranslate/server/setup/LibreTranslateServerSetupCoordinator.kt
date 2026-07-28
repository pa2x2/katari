package mihon.translation.provider.libretranslate.server.setup

import kotlinx.coroutines.CancellationException
import mihon.translation.provider.libretranslate.protocol.LibreTranslateService
import mihon.translation.provider.libretranslate.server.LibreTranslateServerConfiguration
import okhttp3.HttpUrl

internal sealed interface LibreTranslateServerSetupResult {
    data object InvalidEndpoint : LibreTranslateServerSetupResult

    data object Ready : LibreTranslateServerSetupResult

    data object ConnectionFailed : LibreTranslateServerSetupResult

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
        val ready = try {
            serviceFactory(endpoint, apiKey).languages().isNotEmpty()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            false
        }
        return try {
            saveConfiguration(endpoint, apiKey, ready)
            if (ready) {
                LibreTranslateServerSetupResult.Ready
            } else {
                LibreTranslateServerSetupResult.ConnectionFailed
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            LibreTranslateServerSetupResult.SaveFailed
        }
    }
}
