package eu.kanade.tachiyomi.di

import android.app.Application
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.network.NetworkHelper
import mihon.app.di.AppGraph
import mihon.entry.interactions.download.markEntryDownloadRuntimeDependenciesRegistered
import mihon.entry.interactions.translate.markEntryTranslateRuntimeDependenciesRegistered
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.feature.runtime.addFeatureRuntimeGraph
import tachiyomi.data.Database
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.get

/** Hands the app graph's services to code that still resolves them through Injekt. */
class AppModule(val app: Application, private val graph: AppGraph) : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addFeatureRuntimeGraph(graph)

        markEntryDownloadRuntimeDependenciesRegistered()
        markEntryTranslateRuntimeDependenciesRegistered()

        // Asynchronously init expensive components for a faster cold start
        ContextCompat.getMainExecutor(app).execute {
            get<NetworkHelper>()

            get<SourceManager>()

            get<Database>()

            graph.featureRuntimeContributions.validate(get<FeatureRuntimeComposition>().evaluation)
            graph.featureRuntimeContributions.warmUp()
        }
    }
}
