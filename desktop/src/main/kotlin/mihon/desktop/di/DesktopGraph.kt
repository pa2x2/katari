package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import eu.kanade.tachiyomi.data.track.TrackerManager
import mihon.app.di.AppUiScope
import mihon.app.ui.di.AppUiGraph
import mihon.data.di.DatabaseBindings
import mihon.data.di.RepositoryBindings

@DependencyGraph(
    AppScope::class,
    additionalScopes = [AppUiScope::class],
    bindingContainers = [
        ApplicationBindings::class,
        StorageBindings::class,
        PreferenceBindings::class,
        NetworkBindings::class,
        DatabaseBindings::class,
        DatabaseDriverBindings::class,
        RepositoryBindings::class,
        ProfileBindings::class,
        SourceBindings::class,
        LibraryUpdateBindings::class,
        DownloadBindings::class,
        TranslationBindings::class,
        TrackerBindings::class,
    ],
)
interface DesktopGraph : AppUiGraph {
    // No desktop screen tracks yet; this keeps the trackers' bindings checked until the tracking screens move.
    val trackerManager: TrackerManager
}
