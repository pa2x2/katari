package app.katari.ui.di

import app.katari.entry.interactions.media.EntryViewerSettingsScreenProjectionResolver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import eu.kanade.presentation.more.settings.screen.productionEntryViewerSettingsScreenProjectionResolver

@ContributesTo(AppScope::class)
@BindingContainer
object ViewerSettingsScreenProjectionBindings {
    @Provides
    fun viewerSettingsScreenProjectionResolver(): EntryViewerSettingsScreenProjectionResolver =
        productionEntryViewerSettingsScreenProjectionResolver()
}
