package mihon.translation.provider.libretranslate.offline.setup

import kotlinx.coroutines.CancellationException
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorConfiguration
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorSettings
import mihon.translation.provider.libretranslate.protocol.LibreTranslateService
import okhttp3.HttpUrl

internal sealed interface OfflineTranslatorSetupResult {
    data object InvalidPort : OfflineTranslatorSetupResult

    data object Ready : OfflineTranslatorSetupResult

    data object ConnectionFailed : OfflineTranslatorSetupResult
}

internal class OfflineTranslatorSetupCoordinator(
    private val settings: OfflineTranslatorSettings,
    private val serviceFactory: (HttpUrl) -> LibreTranslateService,
) {
    suspend fun test(portText: String): OfflineTranslatorSetupResult {
        val port = OfflineTranslatorConfiguration.parsePort(portText)
            ?: return OfflineTranslatorSetupResult.InvalidPort
        settings.port = port
        return try {
            if (serviceFactory(settings.endpoint()).languages().isNotEmpty()) {
                OfflineTranslatorSetupResult.Ready
            } else {
                OfflineTranslatorSetupResult.ConnectionFailed
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            OfflineTranslatorSetupResult.ConnectionFailed
        }
    }
}
