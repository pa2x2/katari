package mihon.app.di

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import mihon.app.di.injekt.InjektHostBindings
import mihon.entry.interactions.host.AppEntryHostBindings
import mihon.feature.runtime.FeatureRuntimeGraph

@DependencyGraph(AppScope::class, bindingContainers = [InjektHostBindings::class, AppEntryHostBindings::class])
interface AppGraph : FeatureRuntimeGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides application: Application): AppGraph
    }
}
