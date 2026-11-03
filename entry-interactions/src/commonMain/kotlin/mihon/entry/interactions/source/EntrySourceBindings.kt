package mihon.entry.interactions.source

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import tachiyomi.domain.source.service.SourceManager

@ContributesTo(AppScope::class)
@BindingContainer
object EntrySourceBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun homeFeature(
        sourceManager: SourceManager,
    ): EntrySourceHomeFeature = DefaultEntrySourceHomeFeature(
        sourceManager = sourceManager,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun coverNetworkFeature(
        sourceManager: SourceManager,
    ): EntryCoverNetworkFeature = DefaultEntryCoverNetworkFeature(
        sourceManager = sourceManager,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun settingsFeature(
        sourceManager: SourceManager,
    ): EntrySourceSettingsFeature = DefaultEntrySourceSettingsFeature(
        sourceManager = sourceManager,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun trackerSourceAdapterFeature(
        sourceManager: SourceManager,
        settings: EntrySourceSettingsFeature,
        home: EntrySourceHomeFeature,
    ): EntryTrackerSourceAdapterFeature = DefaultEntryTrackerSourceAdapterFeature(
        sourceManager = sourceManager,
        settings = settings,
        home = home,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun webViewFeature(
        sourceManager: SourceManager,
    ): EntryWebViewFeature = DefaultEntryWebViewFeature(
        sourceManager = sourceManager,
    )
}
