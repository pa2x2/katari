package eu.kanade.tachiyomi.ui.main

import mihon.entry.interactions.EntryMediaCacheBucketKeys

internal fun enabledMediaCacheBucketsForLaunchClear(
    autoClearEntryPageImageCache: Boolean,
    autoClearAnimePlaybackCache: Boolean,
): List<String> = buildList {
    if (autoClearEntryPageImageCache) {
        add(EntryMediaCacheBucketKeys.MANGA_PAGE_IMAGE)
    }
    if (autoClearAnimePlaybackCache) {
        add(EntryMediaCacheBucketKeys.ANIME_PLAYBACK)
    }
}
