package mihon.entry.interactions.catalogue

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.catalogue.host.EntryCatalogueProviderHost
import mihon.entry.interactions.catalogue.host.EntrySourceDescriptionAdapter
import mihon.entry.interactions.catalogue.host.SourceManagerEntryCatalogueProviderHost
import mihon.entry.interactions.catalogue.runtime.DefaultEntryCatalogueFeature
import mihon.entry.interactions.catalogue.runtime.EntryCatalogueGraphStateValidator
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.feature.runtime.injekt.InjektExport
import mihon.feature.runtime.injekt.injektExport
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.source.service.EntrySourceDescriptionResolutionPort
import tachiyomi.domain.source.service.SourceManager

@ContributesTo(AppScope::class)
@BindingContainer
object EntryCatalogueBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun providerHost(sourceManager: SourceManager): EntryCatalogueProviderHost =
        SourceManagerEntryCatalogueProviderHost(sourceManager)

    @Provides
    @SingleIn(AppScope::class)
    internal fun graphStateValidator(composition: FeatureRuntimeComposition): EntryCatalogueGraphStateValidator =
        EntryCatalogueGraphStateValidator(composition.evaluation)

    @Provides
    @SingleIn(AppScope::class)
    internal fun feature(
        host: EntryCatalogueProviderHost,
        graphStateValidator: EntryCatalogueGraphStateValidator,
        networkToLocalEntry: NetworkToLocalEntry,
    ): EntryCatalogueFeature = DefaultEntryCatalogueFeature(host, graphStateValidator, networkToLocalEntry)

    @Provides
    @SingleIn(AppScope::class)
    internal fun sourceDescription(
        host: EntryCatalogueProviderHost,
        graphStateValidator: EntryCatalogueGraphStateValidator,
    ): EntrySourceDescriptionResolutionPort = EntrySourceDescriptionAdapter(host, graphStateValidator)

    @Provides
    @IntoSet
    fun graphContributor(): FeatureGraphContributor = EntryCatalogueFeatureContributor

    @Provides
    @IntoSet
    internal fun exportProviderHost(host: () -> EntryCatalogueProviderHost): InjektExport<*> = injektExport(host)

    @Provides
    @IntoSet
    internal fun exportGraphStateValidator(validator: () -> EntryCatalogueGraphStateValidator): InjektExport<*> =
        injektExport(validator)

    @Provides
    @IntoSet
    fun exportFeature(feature: () -> EntryCatalogueFeature): InjektExport<*> = injektExport(feature)

    @Provides
    @IntoSet
    fun exportSourceDescription(sourceDescription: () -> EntrySourceDescriptionResolutionPort): InjektExport<*> =
        injektExport(sourceDescription)
}
