package mihon.entry.interactions.catalogue

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.catalogue.host.EntryCatalogueProviderHost
import mihon.entry.interactions.catalogue.host.EntrySourceDescriptionAdapter
import mihon.entry.interactions.catalogue.runtime.DefaultEntryCatalogueFeature
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.source.service.EntrySourceDescriptionResolutionPort

@ContributesTo(AppScope::class)
@BindingContainer
object EntryCatalogueBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun feature(
        host: EntryCatalogueProviderHost,
        networkToLocalEntry: NetworkToLocalEntry,
    ): EntryCatalogueFeature = DefaultEntryCatalogueFeature(host, networkToLocalEntry)

    @Provides
    @SingleIn(AppScope::class)
    internal fun sourceDescription(
        host: EntryCatalogueProviderHost,
    ): EntrySourceDescriptionResolutionPort = EntrySourceDescriptionAdapter(host)
}
