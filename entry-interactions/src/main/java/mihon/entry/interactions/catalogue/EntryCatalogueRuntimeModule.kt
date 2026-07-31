package mihon.entry.interactions.catalogue

import mihon.entry.interactions.catalogue.host.EntryCatalogueProviderHost
import mihon.entry.interactions.catalogue.host.EntrySourceDescriptionAdapter
import mihon.entry.interactions.catalogue.host.SourceManagerEntryCatalogueProviderHost
import mihon.entry.interactions.catalogue.runtime.DefaultEntryCatalogueFeature
import mihon.entry.interactions.catalogue.runtime.EntryCatalogueGraphStateValidator
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeArtifacts
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeModule
import mihon.entry.interactions.runtime.production.entryFeatureRuntimeBoundary
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
    addSingletonFactory<EntrySourceDescriptionResolutionPort> {
        EntrySourceDescriptionAdapter(
            get(),
            get(),
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryCatalogueFeature>() }),
    )
}
