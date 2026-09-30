package mihon.translation.provider.deepl

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.translation.provider.deepl.protocol.DeepLLanguages
import mihon.translation.provider.deepl.protocol.DeepLService
import mihon.translation.provider.server.ServerConnectionSettings

/**
 * Remembers the languages of the configured server for a while, so that readiness, which is checked before every
 * page, does not ask the server for them each time. Another endpoint or API key is asked anew, and so is the server
 * when what is remembered is [refreshed] because it lacks a language.
 */
internal class DeepLLanguageCatalog(
    private val settings: ServerConnectionSettings,
    private val now: () -> Long = System::nanoTime,
) {
    private val mutex = Mutex()
    private var remembered: Remembered? = null

    suspend fun languages(service: DeepLService): DeepLLanguages = mutex.withLock {
        val connection = connection()
        remembered
            ?.takeIf { it.connection == connection && now() - it.readAt < VALIDITY_NANOS }
            ?.let { return it.languages }
        service.languages().also { remembered = Remembered(connection, it, now()) }
    }

    /** The languages the server has now, or null when [known] were read from it only moments ago. */
    suspend fun refreshed(service: DeepLService, known: DeepLLanguages): DeepLLanguages? = mutex.withLock {
        val connection = connection()
        val latest = remembered?.takeIf { it.connection == connection }
        if (latest != null && latest.languages !== known) return latest.languages
        if (latest != null && now() - latest.readAt < FRESH_NANOS) return null
        service.languages().also { remembered = Remembered(connection, it, now()) }
    }

    private fun connection(): Pair<String?, String?> = settings.endpoint?.toString() to settings.apiKey

    private class Remembered(
        val connection: Pair<String?, String?>,
        val languages: DeepLLanguages,
        val readAt: Long,
    )

    private companion object {
        const val VALIDITY_NANOS = 10 * 60 * 1_000_000_000L

        /** How long after reading them languages count as what the server has now. */
        const val FRESH_NANOS = 5 * 1_000_000_000L
    }
}
