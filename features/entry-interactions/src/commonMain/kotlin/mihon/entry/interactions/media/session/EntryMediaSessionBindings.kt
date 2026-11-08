package mihon.entry.interactions.media.session

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.download.EntryDownloadLifecycleFeature
import mihon.entry.interactions.download.EntryDownloadProcessor
import mihon.entry.interactions.history.EntryHistoryFeature
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import mihon.entry.interactions.state.EntryProgressFeature
import mihon.entry.interactions.state.EntryProgressProcessor
import mihon.entry.interactions.translate.EntryTranslateProcessor
import mihon.entry.interactions.translate.ahead.EntryTranslateAhead
import tachiyomi.domain.entry.repository.EntryRepository

@ContributesTo(AppScope::class)
@BindingContainer
object EntryMediaSessionBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun eventSink(feature: () -> EntryMediaSessionFeature): EntryMediaSessionEventSink =
        EntryMediaSessionEventSink { event ->
            feature().onEvent(event)
        }

    @Provides
    @SingleIn(AppScope::class)
    internal fun feature(
        mediaSessionProcessors: Map<EntryType, EntryMediaSessionProcessor>,
        progressProcessors: Map<EntryType, EntryProgressProcessor>,
        downloadProcessors: Map<EntryType, EntryDownloadProcessor>,
        translateProcessors: Map<EntryType, EntryTranslateProcessor>,
        incognitoState: EntryMediaSessionIncognitoState,
        entryRepository: () -> EntryRepository,
        progress: () -> EntryProgressFeature,
        downloads: () -> EntryDownloadLifecycleFeature,
        history: () -> EntryHistoryFeature,
        translateAhead: () -> EntryTranslateAhead,
        settledListeners: () -> Set<EntryMediaSessionSettledListener>,
    ): EntryMediaSessionFeature = DefaultEntryMediaSessionFeature(
        types = EntryMediaSessionTypes(
            mediaSession = mediaSessionProcessors.keys,
            progress = progressProcessors.keys,
            download = downloadProcessors.keys,
            translateAhead = translateProcessors.keys intersect downloadProcessors.keys,
        ),
        incognitoState = incognitoState,
        entryRepository = entryRepository,
        progress = progress,
        downloads = downloads,
        history = history,
        translateAhead = translateAhead,
        settledListeners = settledListeners,
    )
}

/** Features that act when media is put down contribute their listeners; a graph may have none. */
@ContributesTo(AppScope::class)
interface EntryMediaSessionSettledBindings {
    @Multibinds(allowEmpty = true)
    fun settledListeners(): Set<EntryMediaSessionSettledListener>
}
