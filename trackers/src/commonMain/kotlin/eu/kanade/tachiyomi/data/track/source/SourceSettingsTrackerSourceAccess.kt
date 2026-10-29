package eu.kanade.tachiyomi.data.track.source

import android.content.SharedPreferences
import dev.zacsweers.metro.Inject
import mihon.entry.interactions.source.EntrySourceSettingsFeature
import mihon.entry.interactions.source.EntrySourceSettingsResolution
import mihon.entry.interactions.source.EntryTrackerSourceAdapterFeature
import mihon.entry.interactions.source.EntryTrackerSourceAdapterResolution

/** The installed extension sources, reached through the entry interactions' source features. */
@Inject
class SourceSettingsTrackerSourceAccess(
    private val settingsFeature: Lazy<EntrySourceSettingsFeature>,
    private val trackerAdapterFeature: Lazy<EntryTrackerSourceAdapterFeature>,
) : TrackerSourceAccess {

    override fun settings(sourceKey: Long): TrackerSourceSettings? {
        val settings = settingsFeature.value.resolve(sourceKey) as? EntrySourceSettingsResolution.Available
        return settings?.preferences?.let(::SharedPreferencesTrackerSourceSettings)
    }

    override fun connection(sourceKey: Long): TrackerSourceConnection? {
        val adapter = trackerAdapterFeature.value.resolve(sourceKey) as? EntryTrackerSourceAdapterResolution.Available
            ?: return null
        return TrackerSourceConnection(
            settings = SharedPreferencesTrackerSourceSettings(adapter.preferences),
            homeUrl = adapter.homeUrl,
            client = adapter.imageClient,
        )
    }
}

private class SharedPreferencesTrackerSourceSettings(
    private val preferences: SharedPreferences,
) : TrackerSourceSettings {
    override fun getString(key: String, defaultValue: String): String? = preferences.getString(key, defaultValue)

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = preferences.getBoolean(key, defaultValue)
}
