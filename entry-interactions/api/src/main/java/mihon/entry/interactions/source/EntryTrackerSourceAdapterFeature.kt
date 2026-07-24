package mihon.entry.interactions

import android.content.SharedPreferences
import okhttp3.OkHttpClient

interface EntryTrackerSourceAdapterFeature {
    fun resolve(sourceId: Long): EntryTrackerSourceAdapterResolution
}

sealed interface EntryTrackerSourceAdapterResolution {
    data class Available(
        val sourceId: Long,
        val preferences: SharedPreferences,
        val homeUrl: String,
        val imageClient: OkHttpClient,
    ) : EntryTrackerSourceAdapterResolution

    data class Unavailable(
        val sourceId: Long,
        val reasons: Set<EntryTrackerSourceAdapterUnavailableReason>,
    ) : EntryTrackerSourceAdapterResolution
    data class Failed(val sourceId: Long, val cause: Throwable) : EntryTrackerSourceAdapterResolution
}

enum class EntryTrackerSourceAdapterUnavailableReason {
    SETTINGS,
    HOME,
    IMAGE_CLIENT,
}
