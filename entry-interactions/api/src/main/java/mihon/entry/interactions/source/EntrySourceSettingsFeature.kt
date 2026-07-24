package mihon.entry.interactions

import android.content.SharedPreferences
import eu.kanade.tachiyomi.source.entry.EntryPreferenceScreen

interface EntrySourceSettingsFeature {
    fun resolve(sourceId: Long): EntrySourceSettingsResolution
    fun supportedSourceIds(): List<Long>
}

sealed interface EntrySourceSettingsResolution {
    val sourceId: Long

    data class Available(
        override val sourceId: Long,
        val preferences: SharedPreferences,
        private val populateScreen: (EntryPreferenceScreen) -> Unit,
    ) : EntrySourceSettingsResolution {
        fun populate(screen: EntryPreferenceScreen): EntrySourceSettingsPopulateResult =
            runCatching { populateScreen(screen) }
                .fold(
                    onSuccess = { EntrySourceSettingsPopulateResult.Populated },
                    onFailure = EntrySourceSettingsPopulateResult::Failed,
                )
    }

    data class Missing(override val sourceId: Long) : EntrySourceSettingsResolution
    data class Unsupported(override val sourceId: Long) : EntrySourceSettingsResolution
    data class Failed(override val sourceId: Long, val cause: Throwable) : EntrySourceSettingsResolution
}

sealed interface EntrySourceSettingsPopulateResult {
    data object Populated : EntrySourceSettingsPopulateResult
    data class Failed(val cause: Throwable) : EntrySourceSettingsPopulateResult
}
