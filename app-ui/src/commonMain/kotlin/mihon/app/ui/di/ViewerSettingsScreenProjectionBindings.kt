package mihon.app.ui.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import eu.kanade.presentation.more.settings.screen.productionEntryViewerSettingsScreenProjectionResolver
import mihon.entry.interactions.media.EntryViewerSettingsScreenProjectionResolver

@ContributesTo(AppScope::class)
@BindingContainer
object ViewerSettingsScreenProjectionBindings {
    @Provides
    fun viewerSettingsScreenProjectionResolver(): EntryViewerSettingsScreenProjectionResolver =
        productionEntryViewerSettingsScreenProjectionResolver()
}
