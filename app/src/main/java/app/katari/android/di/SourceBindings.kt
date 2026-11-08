package app.katari.android.di

import app.katari.core.platform.PlatformContext
import app.katari.entry.interactions.source.EntrySourceSettingsFeature
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.ui.browse.source.preferences.SourcePreferenceHosts

/** Source services that need the app's own code or an explicit constructor call. */
@BindingContainer
object SourceBindings {
    // A source's preferences are built with the activity showing them, as in androidx's own screen. On Android the
    // screen's PlatformContext is that activity.
    @Provides
    fun sourcePreferenceHosts(settingsFeature: EntrySourceSettingsFeature): SourcePreferenceHosts =
        SourcePreferenceHosts(settingsFeature) { context: PlatformContext -> context }
}
