package mihon.entry.interactions.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.download.EntryDownloadCapability
import mihon.entry.interactions.download.EntryDownloadProcessor
import mihon.entry.interactions.media.EntryMediaSessionCapability
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import mihon.entry.interactions.state.EntryProgressCapability
import mihon.entry.interactions.state.EntryProgressProcessor
import mihon.entry.interactions.translate.EntryTranslateCapability
import mihon.entry.interactions.translate.EntryTranslateProcessor

/** Each capability's providers keyed by the type that contributes them, read from the type modules' plugins. */
@ContributesTo(AppScope::class)
@BindingContainer
object EntryInteractionProviderMapBindings {
    @Provides
    fun mediaSessionProcessors(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryMediaSessionProcessor> =
        plugins.providersOf(EntryMediaSessionCapability)

    @Provides
    fun progressProcessors(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryProgressProcessor> =
        plugins.providersOf(EntryProgressCapability)

    @Provides
    fun downloadProcessors(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryDownloadProcessor> =
        plugins.providersOf(EntryDownloadCapability)

    @Provides
    fun childGroupFilterProcessors(
        plugins: Set<EntryInteractionPlugin>,
    ): Map<EntryType, EntryChildGroupFilterProcessor> = plugins.providersOf(EntryChildGroupFilterCapability)

    @Provides
    fun translateProcessors(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryTranslateProcessor> =
        plugins.providersOf(EntryTranslateCapability)
}
