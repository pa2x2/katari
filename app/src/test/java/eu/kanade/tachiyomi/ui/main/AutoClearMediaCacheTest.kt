package eu.kanade.tachiyomi.ui.main

import io.kotest.matchers.collections.shouldContainExactly
import mihon.entry.interactions.EntryMediaCacheBucketKeys
import org.junit.jupiter.api.Test

class AutoClearMediaCacheTest {

    @Test
    fun `launch clear selects each cache bucket independently`() {
        enabledMediaCacheBucketsForLaunchClear(
            autoClearEntryPageImageCache = false,
            autoClearAnimePlaybackCache = false,
        ).shouldContainExactly(emptyList())

        enabledMediaCacheBucketsForLaunchClear(
            autoClearEntryPageImageCache = true,
            autoClearAnimePlaybackCache = false,
        ).shouldContainExactly(EntryMediaCacheBucketKeys.MANGA_PAGE_IMAGE)

        enabledMediaCacheBucketsForLaunchClear(
            autoClearEntryPageImageCache = false,
            autoClearAnimePlaybackCache = true,
        ).shouldContainExactly(EntryMediaCacheBucketKeys.ANIME_PLAYBACK)

        enabledMediaCacheBucketsForLaunchClear(
            autoClearEntryPageImageCache = true,
            autoClearAnimePlaybackCache = true,
        ).shouldContainExactly(
            EntryMediaCacheBucketKeys.MANGA_PAGE_IMAGE,
            EntryMediaCacheBucketKeys.ANIME_PLAYBACK,
        )
    }
}
