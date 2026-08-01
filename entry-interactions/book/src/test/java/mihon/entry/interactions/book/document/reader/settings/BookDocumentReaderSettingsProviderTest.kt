package mihon.entry.interactions.book.document.reader.settings

import io.kotest.matchers.shouldBe
import mihon.entry.interactions.book.reader.translation.BookAutomaticTranslationPreferences
import mihon.entry.interactions.book.reader.translation.BookAutomaticTranslationSettingsProvider
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationPreferences
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.shared.StandardReaderSharedSettingIds
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class BookDocumentReaderSettingsProviderTest {
    @Test
    fun `shared general settings are entry override definitions backed by profile preferences`() {
        val store = InMemoryPreferenceStore()
        val readerPreferences = BookDocumentReaderPreferences(store)
        val preparationPreferences = ReaderChapterPreparationPreferences(store)
        val translationPreferences = BookAutomaticTranslationPreferences(store)
        val preparationPreference = preparationPreferences.prepareNextChapter(
            BookDocumentReaderSettingsProvider.PROVIDER_ID,
        ).apply { set(true) }
        val translationPreference = translationPreferences.automaticSelectionEnabled(
            BookDocumentReaderSettingsProvider.PROVIDER_ID,
        ).apply { set(true) }
        val provider = BookDocumentReaderSettingsProvider(
            preferences = readerPreferences,
            chapterPreparationPreferences = preparationPreferences,
            automaticTranslationPreferences = translationPreferences,
        )

        provider.settings shouldBe listOf(
            provider.themeModeSetting,
            provider.prepareNextChapterSetting,
            provider.automaticTranslationSetting,
        )
        provider.sharedSettingDefinitions shouldBe mapOf(
            StandardReaderSharedSettingIds.NextChapterPreparation to provider.prepareNextChapterSetting,
            BookAutomaticTranslationSettingsProvider.AUTOMATIC_SELECTION_SETTING_ID to
                provider.automaticTranslationSetting,
        )
        provider.sharedSettingDefinitions.values.forEach { definition ->
            definition.scope shouldBe ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE
        }
        provider.prepareNextChapterSetting.profilePreference shouldBe
            preparationPreference
        provider.automaticTranslationSetting.profilePreference shouldBe
            translationPreference
        provider.prepareNextChapterSetting.profilePreference.get() shouldBe true
        provider.automaticTranslationSetting.profilePreference.get() shouldBe true
    }
}
