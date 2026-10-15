package mihon.app.di

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import mihon.app.di.injekt.InjektHostBindings

@DependencyGraph(AppScope::class, bindingContainers = [InjektHostBindings::class])
interface AppGraph : ApplicationFeatureGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides application: Application): AppGraph
    }
}
