package mihon.entry.interactions.book.document.reader.settings

import mihon.entry.interactions.book.reader.translation.BookAutomaticTranslationPreferences
import mihon.entry.interactions.book.reader.translation.BookAutomaticTranslationSettingsProvider
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationPreferences
import mihon.entry.viewer.settings.ViewerSettingCodecs
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.ViewerSettingsCategory
import mihon.entry.viewer.settings.ViewerSettingsProvider
import mihon.entry.viewer.settings.shared.StandardReaderSharedSettingIds

class BookDocumentReaderSettingsProvider internal constructor(
    preferences: BookDocumentReaderPreferences,
    chapterPreparationPreferences: ReaderChapterPreparationPreferences,
    automaticTranslationPreferences: BookAutomaticTranslationPreferences,
) : ViewerSettingsProvider {
    override val id = PROVIDER_ID
    override val category = ViewerSettingsCategory.READER
    override val displayName = "Book reader"

    val themeModeSetting = ViewerSettingDefinition(
        id = ViewerSettingId(id, BookDocumentReaderPreferences.THEME_MODE_KEY),
        scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
        processorDefault = BookDocumentReaderThemeMode.APP,
        profilePreference = preferences.themeMode,
        codec = ViewerSettingCodecs.codec(
            encode = BookDocumentReaderThemeMode::name,
            decode = { encoded -> BookDocumentReaderThemeMode.entries.firstOrNull { it.name == encoded } },
        ),
    )

    val prepareNextChapterSetting = ViewerSettingDefinition(
        id = ViewerSettingId(id, StandardReaderSharedSettingIds.NextChapterPreparation.value),
        scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
        processorDefault = false,
        profilePreference = chapterPreparationPreferences.prepareNextChapter(PROVIDER_ID),
        codec = ViewerSettingCodecs.Boolean,
    )

    val automaticTranslationSetting = ViewerSettingDefinition(
        id = ViewerSettingId(id, BookAutomaticTranslationSettingsProvider.AUTOMATIC_SELECTION_SETTING_ID.value),
        scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
        processorDefault = false,
        profilePreference = automaticTranslationPreferences.automaticSelectionEnabled(PROVIDER_ID),
        codec = ViewerSettingCodecs.Boolean,
    )

    override val settings = listOf(
        themeModeSetting,
        prepareNextChapterSetting,
        automaticTranslationSetting,
    )

    override val sharedSettingDefinitions = mapOf(
        StandardReaderSharedSettingIds.NextChapterPreparation to prepareNextChapterSetting,
        BookAutomaticTranslationSettingsProvider.AUTOMATIC_SELECTION_SETTING_ID to automaticTranslationSetting,
    )

    companion object {
        const val PROVIDER_ID = "builtin.book.document"
    }
}
