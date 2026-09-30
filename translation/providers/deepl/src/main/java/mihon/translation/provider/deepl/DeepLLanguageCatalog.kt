package mihon.translation.provider.deepl

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.translation.provider.deepl.protocol.DeepLLanguages
import mihon.translation.provider.deepl.protocol.DeepLService
import mihon.translation.provider.server.ServerConnectionSettings

/**
 * Remembers the languages of the configured server for a while, so that readiness, which is checked before every
 * page, does not ask the server for them each time. Another endpoint or API key is asked anew.
 */
internal class DeepLLanguageCatalog(
    private val settings: ServerConnectionSettings,
    private val now: () -> Long = System::nanoTime,
) {
    private val mutex = Mutex()
    private var remembered: Remembered? = null

    suspend fun languages(service: DeepLService): DeepLLanguages = mutex.withLock {
        val connection = settings.endpoint?.toString() to settings.apiKey
        remembered
            ?.takeIf { it.connection == connection && now() - it.readAt < VALIDITY_NANOS }
            ?.let { return it.languages }
        service.languages().also { remembered = Remembered(connection, it, now()) }
    }

    private class Remembered(
        val connection: Pair<String?, String?>,
        val languages: DeepLLanguages,
        val readAt: Long,
    )

    private companion object {
        const val VALIDITY_NANOS = 10 * 60 * 1_000_000_000L
    }
}
