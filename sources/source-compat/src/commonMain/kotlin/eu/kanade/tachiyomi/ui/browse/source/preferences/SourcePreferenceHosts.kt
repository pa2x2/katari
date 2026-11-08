package eu.kanade.tachiyomi.ui.browse.source.preferences

import android.content.Context
import app.katari.core.platform.PlatformContext
import app.katari.entry.interactions.source.EntrySourceSettingsFeature
import app.katari.entry.interactions.source.EntrySourceSettingsResolution

/**
 * Opens sources' settings for the settings screen.
 *
 * @param extensionContext the Android context a source's preferences are built with for a screen shown in a
 * [PlatformContext]: on Android the screen's own activity, on desktop the extension host's application.
 */
class SourcePreferenceHosts(
    private val settingsFeature: EntrySourceSettingsFeature,
    private val extensionContext: (PlatformContext) -> Context,
) {
    /** Null when [sourceId] has no settings to show; the caller releases the host when its screen leaves. */
    fun open(sourceId: Long, context: PlatformContext): SourcePreferenceHost? =
        when (val settings = settingsFeature.resolve(sourceId)) {
            is EntrySourceSettingsResolution.Available -> SourcePreferenceHost(extensionContext(context), settings)
            else -> null
        }
}
