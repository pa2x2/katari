package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.ui.browse.source.preferences.SourcePreferenceHosts
import mihon.core.platform.PlatformContext
import mihon.entry.interactions.source.EntrySourceSettingsFeature

/** Source services that need the app's own code or an explicit constructor call. */
@BindingContainer
object SourceBindings {
    // A source's preferences are built with the activity showing them, as in androidx's own screen. On Android the
    // screen's PlatformContext is that activity.
    @Provides
    fun sourcePreferenceHosts(settingsFeature: EntrySourceSettingsFeature): SourcePreferenceHosts =
        SourcePreferenceHosts(settingsFeature) { context: PlatformContext -> context }
}
