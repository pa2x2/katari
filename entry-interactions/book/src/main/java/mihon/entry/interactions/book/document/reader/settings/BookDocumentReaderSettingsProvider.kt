package mihon.entry.interactions.book.document.reader.settings

import mihon.entry.viewer.settings.ViewerSettingCodecs
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.ViewerSettingsCategory
import mihon.entry.viewer.settings.ViewerSettingsProvider
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

class BookDocumentReaderSettingsProvider(
    preferenceStore: PreferenceStore,
) : ViewerSettingsProvider {
    override val id = PROVIDER_ID
    override val category = ViewerSettingsCategory.READER
    override val displayName = "Book reader"

    private val themeMode = preferenceStore.getEnum(THEME_MODE_KEY, BookDocumentReaderThemeMode.APP)

    val themeModeSetting = ViewerSettingDefinition(
        id = ViewerSettingId(id, THEME_MODE_KEY),
        scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
        processorDefault = BookDocumentReaderThemeMode.APP,
        profilePreference = themeMode,
        codec = ViewerSettingCodecs.codec(
            encode = BookDocumentReaderThemeMode::name,
            decode = { encoded -> BookDocumentReaderThemeMode.entries.firstOrNull { it.name == encoded } },
        ),
    )

    override val settings = listOf(themeModeSetting)

    companion object {
        const val PROVIDER_ID = "builtin.book.document"
        const val KEY_PREFIX = "book_document_reader_"
        const val THEME_MODE_KEY = "book_document_reader_theme_mode"
    }
}
