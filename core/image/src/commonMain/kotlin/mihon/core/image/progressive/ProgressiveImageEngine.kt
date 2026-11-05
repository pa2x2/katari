package mihon.core.image.progressive

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.Closeable

/**
 * Decodes images while they download, so a reader can draw a page before its last byte arrives. A session is opened
 * per image and fed the downloaded bytes; where the engine is off or busy, [openSession] gives none and the image is
 * read once it has downloaded.
 */
class ProgressiveImageEngine(
    private val preferences: ProgressiveImagePreferences,
) : Closeable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sessionsLock = Any()
    private val managedSessions = mutableMapOf<ProgressiveImageSession, Job>()
    private var acceptingSessions = preferences.enabled.get()

    val isEnabled: Boolean
        get() = preferences.enabled.get()

    init {
        scope.launch {
            preferences.enabled.changes()
                .distinctUntilChanged()
                .collect(::applyPreference)
        }
    }

    fun openSession(
        options: ProgressiveImageDecodeOptions = ProgressiveImageDecodeOptions(),
    ): ProgressiveImageSession? {
        return synchronized(sessionsLock) {
            if (!acceptingSessions || !preferences.enabled.get()) return@synchronized null
            if (managedSessions.size >= MAXIMUM_ADMITTED_SESSIONS) return@synchronized null
            val decoder = try {
                openProgressiveDecoder(options)
            } catch (error: Throwable) {
                logcat(LogPriority.WARN, error) { "Failed to create progressive image decoder" }
                return@synchronized null
            }
            ProgressiveImageSession(decoder, ::removeSession).also { session ->
                managedSessions[session] = scope.launch {
                    session.subscriptionCount
                        .collect { session.releaseIfUnobservedAndTerminal() }
                }
            }
        }
    }

    fun disableActiveSessions() {
        val sessions = synchronized(sessionsLock) {
            acceptingSessions = false
            managedSessions.keys.toList()
        }
        sessions.forEach(ProgressiveImageSession::disable)
    }

    override fun close() {
        val sessions = synchronized(sessionsLock) {
            acceptingSessions = false
            managedSessions.toList().also { managedSessions.clear() }
        }
        sessions.forEach { (session, monitor) ->
            monitor.cancel()
            session.close()
        }
        scope.cancel()
    }

    private fun applyPreference(enabled: Boolean) {
        val sessions = synchronized(sessionsLock) {
            acceptingSessions = enabled
            if (enabled) emptyList() else managedSessions.keys.toList()
        }
        sessions.forEach(ProgressiveImageSession::disable)
    }

    private fun removeSession(session: ProgressiveImageSession) {
        val monitor = synchronized(sessionsLock) { managedSessions.remove(session) }
        monitor?.cancel()
    }
}

/**
 * A session can retain up to 64 MiB of decoded animation frames in addition to
 * its native decoder surfaces. Requests that cannot obtain the process-wide
 * slot continue through the existing completed-file pipeline.
 */
private const val MAXIMUM_ADMITTED_SESSIONS = 1
