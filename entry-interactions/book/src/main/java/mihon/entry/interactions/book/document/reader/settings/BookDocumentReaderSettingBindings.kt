package mihon.entry.interactions.book.document.reader.settings

import mihon.entry.interactions.book.reader.translation.BookAutomaticTranslationSettingsProvider
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.entry.viewer.settings.shared.ReaderSharedSettingId
import mihon.entry.viewer.settings.shared.StandardReaderSharedSettingIds

internal class BookDocumentReaderSettingBindings private constructor(
    val themeMode: ViewerSettingBinding<BookDocumentReaderThemeMode>,
    val showStatusBar: ViewerSettingBinding<Boolean>,
    val showReadingProgress: ViewerSettingBinding<Boolean>,
    val readingProgressStyle: ViewerSettingBinding<BookDocumentReaderProgressStyle>,
    val sharedSettings: Map<ReaderSharedSettingId, ViewerSettingBinding<Boolean>>,
) {
    val prepareNextChapter: ViewerSettingBinding<Boolean>
        get() = sharedSettings.getValue(StandardReaderSharedSettingIds.NextChapterPreparation)

    val automaticTranslation: ViewerSettingBinding<Boolean>
        get() = sharedSettings.getValue(BookAutomaticTranslationSettingsProvider.AUTOMATIC_SELECTION_SETTING_ID)

    companion object {
        fun create(
            provider: BookDocumentReaderSettingsProvider,
            binder: ViewerSettingBinder,
            entryId: Long,
        ): BookDocumentReaderSettingBindings {
            return BookDocumentReaderSettingBindings(
                themeMode = binder.bind(provider.themeModeSetting, entryId),
                showStatusBar = binder.bind(provider.showStatusBarSetting, entryId),
                showReadingProgress = binder.bind(provider.showReadingProgressSetting, entryId),
                readingProgressStyle = binder.bind(provider.readingProgressStyleSetting, entryId),
                sharedSettings = provider.sharedSettingDefinitions.mapValues { (_, definition) ->
                    binder.bind(definition, entryId)
                },
            )
        }
    }
}
