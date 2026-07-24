package mihon.entry.interactions.settings

import mihon.entry.interactions.EntryPreviewSize
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.coerceIn
import tachiyomi.core.common.preference.getEnum

class EntryInteractionPreferences(
    preferenceStore: PreferenceStore,
) {
    companion object {
        val PREVIEW_PAGE_COUNT_RANGE = 1..50

        const val ENABLE_MANGA_PREVIEW_KEY = "enable_manga_preview"
        const val MANGA_PREVIEW_PAGE_COUNT_KEY = "manga_preview_page_count"
        const val MANGA_PREVIEW_SIZE_KEY = "manga_preview_size"
        const val ENABLE_ANIME_PREVIEW_KEY = "enable_anime_preview"
        const val ANIME_PREVIEW_PAGE_COUNT_KEY = "anime_preview_page_count"
        const val ANIME_PREVIEW_SIZE_KEY = "anime_preview_size"

        val profileKeys = setOf(
            ENABLE_MANGA_PREVIEW_KEY,
            MANGA_PREVIEW_PAGE_COUNT_KEY,
            MANGA_PREVIEW_SIZE_KEY,
            ENABLE_ANIME_PREVIEW_KEY,
            ANIME_PREVIEW_PAGE_COUNT_KEY,
            ANIME_PREVIEW_SIZE_KEY,
        )
    }

    val enableMangaPreview: Preference<Boolean> = preferenceStore.getBoolean(
        ENABLE_MANGA_PREVIEW_KEY,
        false,
    )

    val mangaPreviewPageCount: Preference<Int> = preferenceStore.getInt(
        MANGA_PREVIEW_PAGE_COUNT_KEY,
        5,
    ).coerceIn(PREVIEW_PAGE_COUNT_RANGE)

    val mangaPreviewSize: Preference<EntryPreviewSize> = preferenceStore.getEnum(
        MANGA_PREVIEW_SIZE_KEY,
        EntryPreviewSize.MEDIUM,
    )

    val enableAnimePreview: Preference<Boolean> = preferenceStore.getBoolean(
        ENABLE_ANIME_PREVIEW_KEY,
        false,
    )

    val animePreviewPageCount: Preference<Int> = preferenceStore.getInt(
        ANIME_PREVIEW_PAGE_COUNT_KEY,
        5,
    ).coerceIn(PREVIEW_PAGE_COUNT_RANGE)

    val animePreviewSize: Preference<EntryPreviewSize> = preferenceStore.getEnum(
        ANIME_PREVIEW_SIZE_KEY,
        EntryPreviewSize.MEDIUM,
    )
}
