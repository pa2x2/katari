package mihon.entry.interactions.anime

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryMediaCacheArtifact
import mihon.entry.interactions.EntryMediaCacheAutoClearPreference
import mihon.entry.interactions.EntryMediaCacheId
import mihon.entry.interactions.EntryMediaCacheProvider
import mihon.entry.interactions.EntryPlayerCache
import tachiyomi.i18n.MR

internal class AnimeMediaCacheProvider(
    playerCache: () -> EntryPlayerCache,
) : EntryMediaCacheProvider {
    override val type = EntryType.ANIME
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
    override val readableSize: String get() = delegate.readableSize
    override fun clear(): Int = delegate.clear()
}
