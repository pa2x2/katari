package eu.kanade.tachiyomi.di

import android.app.Application
import mihon.app.di.AppGraph
import mihon.feature.runtime.addFeatureRuntimeGraph
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.get

/** Hands the app graph's services to code that still resolves them through Injekt. */
class AppModule(val app: Application, private val graph: AppGraph) : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addFeatureRuntimeGraph(graph)
    }
}
