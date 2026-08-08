package mihon.core.common.image.progressive

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.decoder.incremental.IncrementalDecodeOptions
import tachiyomi.decoder.incremental.IncrementalImageDecoder
import java.io.Closeable

class ProgressiveImageEngine(
    private val preferences: ProgressiveImagePreferences,
) : Closeable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sessionsLock = Any()
    private val activeSessions = mutableSetOf<ProgressiveImageSession>()
    private var acceptingSessions = preferences.enabled.get()

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
            val decoder = try {
                IncrementalImageDecoder.newInstance(
                    IncrementalDecodeOptions(
                        preferredOutputWidth = options.preferredOutputWidth,
                        maximumBitmapPixels = options.maximumBitmapPixels,
                        displayProfile = options.displayProfile,
                    ),
                )
            } catch (error: Throwable) {
                logcat(LogPriority.WARN, error) { "Failed to create progressive image decoder" }
                return@synchronized null
            }
            ProgressiveImageSession(decoder, ::removeSession).also(activeSessions::add)
        }
    }

    fun disableActiveSessions() {
        val sessions = synchronized(sessionsLock) {
            acceptingSessions = false
            activeSessions.toList()
        }
        sessions.forEach(ProgressiveImageSession::disable)
    }

    override fun close() {
        val sessions = synchronized(sessionsLock) {
            acceptingSessions = false
            activeSessions.toList().also { activeSessions.clear() }
        }
        sessions.forEach(ProgressiveImageSession::close)
        scope.cancel()
    }

    private fun applyPreference(enabled: Boolean) {
        val sessions = synchronized(sessionsLock) {
            acceptingSessions = enabled
            if (enabled) emptyList() else activeSessions.toList()
        }
        sessions.forEach(ProgressiveImageSession::disable)
    }

    private fun removeSession(session: ProgressiveImageSession) {
        synchronized(sessionsLock) {
            activeSessions.remove(session)
        }
    }
}
