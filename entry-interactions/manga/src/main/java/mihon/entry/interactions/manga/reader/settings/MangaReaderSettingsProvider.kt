package mihon.entry.interactions.manga.reader.settings

import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationPreferences
import mihon.entry.interactions.reader.settings.MangaReaderColorFilterDefinitions
import mihon.entry.interactions.reader.settings.MangaReaderDisplayDefinitions
import mihon.entry.interactions.reader.settings.MangaReaderEInkDefinitions
import mihon.entry.interactions.reader.settings.MangaReaderNavigationDefinitions
import mihon.entry.interactions.reader.settings.MangaReaderPagerDefinitions
import mihon.entry.interactions.reader.settings.MangaReaderReadingDefinitions
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.reader.settings.MangaReaderWebtoonDefinitions
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingsCategory
import mihon.entry.viewer.settings.shared.ReaderSharedSettingId
import mihon.entry.viewer.settings.shared.StandardReaderSharedSettingIds
import tachiyomi.core.common.preference.PreferenceStore

internal class MangaReaderSettingsProvider(
    preferenceStore: PreferenceStore,
    chapterPreparationPreferences: ReaderChapterPreparationPreferences,
) : MangaReaderSettings {

    override val id: String = MangaReaderSettings.PROVIDER_ID
    override val category: ViewerSettingsCategory = ViewerSettingsCategory.READER
    override val displayName: String = "Manga reader"

    private val preferences = MangaReaderPreferences(preferenceStore)

    override val reading: MangaReaderReadingDefinitions = mangaReaderReadingDefinitions(
        preferences = preferences,
        chapterPreparationPreferences = chapterPreparationPreferences,
    )
    override val display: MangaReaderDisplayDefinitions = mangaReaderDisplayDefinitions(preferences)
    override val eInk: MangaReaderEInkDefinitions = mangaReaderEInkDefinitions(preferences)
    override val pager: MangaReaderPagerDefinitions = mangaReaderPagerDefinitions(preferences)
    override val webtoon: MangaReaderWebtoonDefinitions = mangaReaderWebtoonDefinitions(preferences)
    override val navigation: MangaReaderNavigationDefinitions = mangaReaderNavigationDefinitions(preferences)
    override val colorFilter: MangaReaderColorFilterDefinitions = mangaReaderColorFilterDefinitions(preferences)

    override val settings: List<ViewerSettingDefinition<*>> by lazy {
        reading.all +
            display.all +
            eInk.all +
            pager.all +
            webtoon.all +
            navigation.all +
            colorFilter.all
    }

    override val sharedSettingDefinitions: Map<ReaderSharedSettingId, ViewerSettingDefinition<Boolean>> by lazy {
        mapOf(StandardReaderSharedSettingIds.NextChapterPreparation to reading.prepareNextChapter)
    }
}
