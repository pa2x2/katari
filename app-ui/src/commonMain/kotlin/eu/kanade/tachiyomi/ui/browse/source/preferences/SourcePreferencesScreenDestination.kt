package eu.kanade.tachiyomi.ui.browse.source.preferences

import app.katari.entry.interactions.source.EntrySourceSettingsFeature
import app.katari.entry.interactions.source.EntrySourceSettingsResolution
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen

@Inject
@ContributesBinding(AppScope::class)
class SourcePreferencesScreenDestination(
    private val entrySourceSettingsFeature: EntrySourceSettingsFeature,
) : SourcePreferencesDestination {
    override fun isAvailable(sourceId: Long): Boolean =
        entrySourceSettingsFeature.resolve(sourceId) is EntrySourceSettingsResolution.Available

    override fun screen(sourceId: Long): Screen = SourcePreferencesScreen(sourceId)
}
