package eu.kanade.domain.track.store

import android.content.Context
import androidx.core.content.edit
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

class DelayedTrackingStore(context: Context) {

    /**
     * Preference file where queued tracking updates are stored.
     */
    private val preferences = context.getSharedPreferences("tracking_queue", Context.MODE_PRIVATE)

    fun add(trackId: Long, progress: Double) {
        val previousProgress = preferences.getFloat(trackId.toString(), 0f)
        if (progress > previousProgress) {
            logcat(LogPriority.DEBUG) { "Queuing track item: $trackId, progress: $progress" }
            preferences.edit {
                putFloat(trackId.toString(), progress.toFloat())
            }
        }
    }

    fun remove(trackId: Long) {
        preferences.edit {
            remove(trackId.toString())
        }
    }

    fun getItems(): List<DelayedTrackingItem> {
        return preferences.all.mapNotNull {
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
