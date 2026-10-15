package mihon.app.di.injekt

import mihon.app.di.EntryFeatureGraph
import mihon.entry.interactions.statistics.EntryStatisticsFeature
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory

/** Serves the graph's Entry Features to code that still resolves them through Injekt. */
fun InjektRegistrar.addEntryFeatures(graph: EntryFeatureGraph) {
    addSingletonFactory<EntryStatisticsFeature> { graph.entryStatisticsFeature }
}
