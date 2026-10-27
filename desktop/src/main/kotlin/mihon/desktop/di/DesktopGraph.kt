package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import mihon.app.di.AppUiScope
import mihon.app.ui.di.AppUiGraph
import mihon.data.di.DatabaseBindings
import mihon.data.di.RepositoryBindings

@DependencyGraph(
    AppScope::class,
    additionalScopes = [AppUiScope::class],
    bindingContainers = [
        StorageBindings::class,
        PreferenceBindings::class,
        DatabaseBindings::class,
        DatabaseDriverBindings::class,
        RepositoryBindings::class,
        ProfileBindings::class,
        LibraryUpdateBindings::class,
    ],
)
interface DesktopGraph : AppUiGraph
