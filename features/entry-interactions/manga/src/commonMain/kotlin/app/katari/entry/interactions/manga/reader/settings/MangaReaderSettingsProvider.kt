package app.katari.entry.interactions.manga.reader.settings

import app.katari.entry.interactions.reader.preparation.ReaderChapterPreparationPreferences
import app.katari.entry.interactions.reader.settings.MangaReaderColorFilterDefinitions
import app.katari.entry.interactions.reader.settings.MangaReaderDisplayDefinitions
import app.katari.entry.interactions.reader.settings.MangaReaderEInkDefinitions
import app.katari.entry.interactions.reader.settings.MangaReaderNavigationDefinitions
import app.katari.entry.interactions.reader.settings.MangaReaderPageTextDefinitions
import app.katari.entry.interactions.reader.settings.MangaReaderPagerDefinitions
import app.katari.entry.interactions.reader.settings.MangaReaderReadingDefinitions
import app.katari.entry.interactions.reader.settings.MangaReaderSettings
import app.katari.entry.interactions.reader.settings.MangaReaderWebtoonDefinitions
import app.katari.entry.viewer.settings.ViewerSettingDefinition
import app.katari.entry.viewer.settings.ViewerSettingsCategory
import app.katari.entry.viewer.settings.shared.ReaderSharedSettingId
import app.katari.entry.viewer.settings.shared.StandardReaderSharedSettingIds
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
    override val pageText: MangaReaderPageTextDefinitions = mangaReaderPageTextDefinitions(preferences)

    override val settings: List<ViewerSettingDefinition<*>> by lazy {
        reading.all +
            display.all +
            eInk.all +
            pager.all +
            webtoon.all +
            navigation.all +
            colorFilter.all +
            pageText.all
    }

    override val sharedSettingDefinitions: Map<ReaderSharedSettingId, ViewerSettingDefinition<Boolean>> by lazy {
        mapOf(StandardReaderSharedSettingIds.NextChapterPreparation to reading.prepareNextChapter)
    }
}
