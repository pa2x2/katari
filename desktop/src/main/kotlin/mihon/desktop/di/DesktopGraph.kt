package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import mihon.app.di.AppUiScope
import mihon.app.ui.di.AppUiGraph

@DependencyGraph(
    AppScope::class,
    additionalScopes = [AppUiScope::class],
    bindingContainers = [
        StorageBindings::class,
        PreferenceBindings::class,
    ],
)
interface DesktopGraph : AppUiGraph
