package eu.kanade.domain.track.store

import logcat.LogPriority
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.system.logcat

/**
 * Tracking updates that failed to reach their service, kept until a retry sends them: each track ID maps to the
 * progress to send. [preferences] holds nothing else.
 */
class DelayedTrackingStore(private val preferences: PreferenceStore) {

    fun add(trackId: Long, progress: Double) {
        val preference = preferences.getFloat(trackId.toString(), 0f)
        if (progress > preference.get()) {
            logcat(LogPriority.DEBUG) { "Queuing track item: $trackId, progress: $progress" }
            preference.set(progress.toFloat())
        }
    }

    fun remove(trackId: Long) {
        preferences.getFloat(trackId.toString(), 0f).delete()
    }

    fun getItems(): List<DelayedTrackingItem> {
        return preferences.getAll().mapNotNull {
            DelayedTrackingItem(
                trackId = it.key.toLong(),
                progress = it.value.toString().toFloat(),
            )
        }
    }

    data class DelayedTrackingItem(
        val trackId: Long,
        val progress: Float,
    )
}
