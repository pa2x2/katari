package app.katari.entry.interactions.anime.media

import app.katari.entry.interactions.media.EntryMediaCacheArtifact
import app.katari.entry.interactions.media.EntryMediaCacheAutoClearPreference
import app.katari.entry.interactions.media.EntryMediaCacheId
import app.katari.entry.interactions.media.EntryMediaCacheProvider
import app.katari.entry.interactions.runtime.EntryPlayerCache
import tachiyomi.i18n.*

internal class AnimeMediaCacheProvider(
    playerCache: () -> EntryPlayerCache,
) : EntryMediaCacheProvider {
    override val artifacts: List<EntryMediaCacheArtifact> = listOf(AnimePlaybackCacheArtifact(playerCache))
}

private class AnimePlaybackCacheArtifact(
    cache: () -> EntryPlayerCache,
) : EntryMediaCacheArtifact {
    private val delegate by lazy(cache)

    override val id = EntryMediaCacheId("anime.playback")
    override val clearLabel = MR.strings.pref_clear_anime_playback_cache
    override val autoClearLabel = MR.strings.pref_auto_clear_anime_playback_cache
    override val autoClearPreference = EntryMediaCacheAutoClearPreference(
        key = "auto_clear_anime_playback_cache",
        seedFromKeyWhenAbsent = "auto_clear_chapter_cache",
    )
    override val sizeBytes: Long get() = delegate.sizeBytes
    override fun clear(): Int = delegate.clear()
}
