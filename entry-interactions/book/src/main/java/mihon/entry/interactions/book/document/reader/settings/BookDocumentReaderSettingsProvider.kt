package mihon.entry.interactions.book.document.reader.settings

import mihon.entry.interactions.book.reader.translation.BookAutomaticTranslationPreferences
import mihon.entry.interactions.book.reader.translation.BookAutomaticTranslationSettingsProvider
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationPreferences
import mihon.entry.interactions.reader.settings.BookDocumentReaderSettings
import mihon.entry.viewer.settings.ViewerSettingCodecs
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.ViewerSettingsCategory
import mihon.entry.viewer.settings.shared.StandardReaderSharedSettingIds

internal class BookDocumentReaderSettingsProvider(
    preferences: BookDocumentReaderPreferences,
    chapterPreparationPreferences: ReaderChapterPreparationPreferences,
    automaticTranslationPreferences: BookAutomaticTranslationPreferences,
) : BookDocumentReaderSettings {
    override val id = BookDocumentReaderSettings.SURFACE_ID
    override val category = ViewerSettingsCategory.READER
    override val displayName = "Book reader"

    override val themeModeSetting = ViewerSettingDefinition(
        id = ViewerSettingId(id, BookDocumentReaderPreferences.THEME_MODE_KEY),
        scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
        processorDefault = BookDocumentReaderThemeMode.APP,
        profilePreference = preferences.themeMode,
        codec = ViewerSettingCodecs.codec(
            encode = BookDocumentReaderThemeMode::name,
            decode = { encoded -> BookDocumentReaderThemeMode.entries.firstOrNull { it.name == encoded } },
        ),
    )

    override val textSizeSetting = ViewerSettingDefinition(
        id = ViewerSettingId(id, BookDocumentReaderPreferences.TEXT_SIZE_PERCENT_KEY),
        scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
        processorDefault = BookDocumentReaderSettings.DEFAULT_TEXT_SIZE_PERCENT,
        profilePreference = preferences.textSizePercent,
        codec = ViewerSettingCodecs.Int,
        validate = BookDocumentReaderSettings::isValidTextSizePercent,
    )

    override val keepScreenAliveSetting = ViewerSettingDefinition(
        id = ViewerSettingId(id, BookDocumentReaderPreferences.KEEP_SCREEN_ALIVE_KEY),
        scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
        processorDefault = BookDocumentReaderSettings.DEFAULT_KEEP_SCREEN_ALIVE,
        profilePreference = preferences.keepScreenAlive,
        codec = ViewerSettingCodecs.Boolean,
    )

    override val showStatusBarSetting = ViewerSettingDefinition(
        id = ViewerSettingId(id, BookDocumentReaderPreferences.SHOW_STATUS_BAR_KEY),
        scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
        processorDefault = false,
        profilePreference = preferences.showStatusBar,
        codec = ViewerSettingCodecs.Boolean,
    )

    override val showNavigationBarSetting = ViewerSettingDefinition(
        id = ViewerSettingId(id, BookDocumentReaderPreferences.SHOW_NAVIGATION_BAR_KEY),
        scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
        processorDefault = false,
        profilePreference = preferences.showNavigationBar,
        codec = ViewerSettingCodecs.Boolean,
    )

    override val showTextSelectionMenuSetting = ViewerSettingDefinition(
        id = ViewerSettingId(id, BookDocumentReaderPreferences.SHOW_TEXT_SELECTION_MENU_KEY),
        scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
        processorDefault = true,
        profilePreference = preferences.showTextSelectionMenu,
        codec = ViewerSettingCodecs.Boolean,
    )

    override val showReadingProgressSetting = ViewerSettingDefinition(
        id = ViewerSettingId(id, BookDocumentReaderPreferences.SHOW_READING_PROGRESS_KEY),
        scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
        processorDefault = true,
        profilePreference = preferences.showReadingProgress,
        codec = ViewerSettingCodecs.Boolean,
    )

    override val readingProgressStyleSetting = ViewerSettingDefinition(
        id = ViewerSettingId(id, BookDocumentReaderPreferences.READING_PROGRESS_STYLE_KEY),
        scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
        processorDefault = BookDocumentReaderProgressStyle.PERCENTAGE,
        profilePreference = preferences.readingProgressStyle,
        codec = ViewerSettingCodecs.codec(
            encode = BookDocumentReaderProgressStyle::name,
            decode = { encoded -> BookDocumentReaderProgressStyle.entries.firstOrNull { it.name == encoded } },
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
        textSizeSetting,
        keepScreenAliveSetting,
        showStatusBarSetting,
        showNavigationBarSetting,
        showTextSelectionMenuSetting,
        showReadingProgressSetting,
        readingProgressStyleSetting,
        prepareNextChapterSetting,
        automaticTranslationSetting,
    )

    override val sharedSettingDefinitions = mapOf(
        StandardReaderSharedSettingIds.NextChapterPreparation to prepareNextChapterSetting,
        BookAutomaticTranslationSettingsProvider.AUTOMATIC_SELECTION_SETTING_ID to automaticTranslationSetting,
    )

    companion object {
        const val PROVIDER_ID = BookDocumentReaderSettings.SURFACE_ID
    }
}
