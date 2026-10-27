package mihon.entry.interactions.catalogue

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.catalogue.host.EntryCatalogueProviderHost
import mihon.entry.interactions.catalogue.host.SourceManagerEntryCatalogueProviderHost
import tachiyomi.domain.source.service.SourceManager

@ContributesTo(AppScope::class)
@BindingContainer
object AndroidEntryCatalogueBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun providerHost(sourceManager: SourceManager): EntryCatalogueProviderHost =
        SourceManagerEntryCatalogueProviderHost(sourceManager)
}
