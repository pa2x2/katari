package eu.kanade.tachiyomi.ui.browse.source.preferences

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen
import mihon.entry.interactions.source.EntrySourceSettingsFeature
import mihon.entry.interactions.source.EntrySourceSettingsResolution

@Inject
@ContributesBinding(AppScope::class)
class SourcePreferencesScreenDestination(
    private val entrySourceSettingsFeature: EntrySourceSettingsFeature,
) : SourcePreferencesDestination {
    override fun isAvailable(sourceId: Long): Boolean =
        entrySourceSettingsFeature.resolve(sourceId) is EntrySourceSettingsResolution.Available

    override fun screen(sourceId: Long): Screen = SourcePreferencesScreen(sourceId)
}
