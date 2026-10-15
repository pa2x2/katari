package mihon.entry.interactions.anime.runtime

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.ui.video.player.AnimePlayerBasePreferences
import eu.kanade.tachiyomi.ui.video.player.ResolveVideoStream
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerMediaCache
import eu.kanade.tachiyomi.ui.video.player.VideoStreamResolver
import mihon.entry.interactions.runtime.EntryPlayerCache
import mihon.feature.runtime.injekt.InjektExport
import mihon.feature.runtime.injekt.injektExport
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository

@ContributesTo(AppScope::class)
@BindingContainer
object AnimePlayerBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun resolveVideoStream(
        videoRepository: EntryRepository,
        entryChapterRepository: EntryChapterRepository,
        playbackPreferencesRepository: PlaybackPreferencesRepository,
    ): ResolveVideoStream = ResolveVideoStream(videoRepository, entryChapterRepository, playbackPreferencesRepository)

    @Provides
    internal fun videoStreamResolver(resolveVideoStream: ResolveVideoStream): VideoStreamResolver = resolveVideoStream

    @Provides
    @SingleIn(AppScope::class)
    internal fun mediaCache(application: Application): VideoPlayerMediaCache = VideoPlayerMediaCache(application)

    @Provides
    internal fun playerCache(cache: VideoPlayerMediaCache): EntryPlayerCache = cache

    @Provides
    @SingleIn(AppScope::class)
    internal fun basePreferences(preferenceStore: PreferenceStore): AnimePlayerBasePreferences =
        AnimePlayerBasePreferences(preferenceStore)

    @Provides
    @IntoSet
    internal fun exportResolveVideoStream(resolveVideoStream: () -> ResolveVideoStream): InjektExport<*> =
        injektExport(resolveVideoStream)

    @Provides
    @IntoSet
    internal fun exportVideoStreamResolver(resolver: () -> VideoStreamResolver): InjektExport<*> =
        injektExport(resolver)

    @Provides
    @IntoSet
    internal fun exportMediaCache(cache: () -> VideoPlayerMediaCache): InjektExport<*> = injektExport(cache)

    @Provides
    @IntoSet
    fun exportPlayerCache(cache: () -> EntryPlayerCache): InjektExport<*> = injektExport(cache)

    @Provides
    @IntoSet
    internal fun exportBasePreferences(preferences: () -> AnimePlayerBasePreferences): InjektExport<*> =
        injektExport(preferences)
}
