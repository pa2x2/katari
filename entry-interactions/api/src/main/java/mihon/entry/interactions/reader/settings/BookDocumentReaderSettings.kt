package mihon.entry.interactions.reader.settings

import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingsProvider

interface BookDocumentReaderSettings : ViewerSettingsProvider {
    val themeModeSetting: ViewerSettingDefinition<BookDocumentReaderThemeMode>
    val showStatusBarSetting: ViewerSettingDefinition<Boolean>
    val showReadingProgressSetting: ViewerSettingDefinition<Boolean>
    val readingProgressStyleSetting: ViewerSettingDefinition<BookDocumentReaderProgressStyle>

    companion object {
        const val SURFACE_ID = "builtin.book.document"
    }
}
