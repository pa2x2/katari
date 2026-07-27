package mihon.entry.interactions

import mihon.feature.runtime.FeatureRuntimeComposition
import tachiyomi.domain.source.service.EntrySourceDescriptionResolutionPort
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryCatalogueFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.catalogue",
    contributor = EntryCatalogueFeatureContributor,
) {
    addSingletonFactory<EntryCatalogueProviderHost> { SourceManagerEntryCatalogueProviderHost(get()) }
    addSingletonFactory { EntryCatalogueGraphStateValidator(get<FeatureRuntimeComposition>().evaluation) }
    addSingletonFactory<EntryCatalogueFeature> {
        DefaultEntryCatalogueFeature(get(), get(), get())
    }
    addSingletonFactory<EntrySourceDescriptionResolutionPort> { EntrySourceDescriptionAdapter(get(), get()) }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryCatalogueFeature>() }),
    )
}
