package mihon.entry.interactions.media

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import mihon.entry.viewer.settings.ViewerSettingCodecs
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.ViewerSettingsCategory
import mihon.entry.viewer.settings.ViewerSettingsProvider
import mihon.entry.viewer.settings.shared.ReaderSharedSettingId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class DefaultEntryViewerSettingsProviderTest {
    @Test
    fun `shared reader settings must be entry override definitions owned by their surface`() {
        val store = InMemoryPreferenceStore()
        val sharedSettingId = ReaderSharedSettingId("reader.shared")
        fun definition(scope: ViewerSettingScope) = ViewerSettingDefinition(
            id = ViewerSettingId("book.document", sharedSettingId.value),
            scope = scope,
            processorDefault = false,
            profilePreference = store.getBoolean("reader.shared", false),
            codec = ViewerSettingCodecs.Boolean,
        )
        fun surface(
            settings: List<ViewerSettingDefinition<*>>,
            sharedDefinition: ViewerSettingDefinition<Boolean>,
        ) = object : ViewerSettingsProvider {
            override val id = "book.document"
            override val category = ViewerSettingsCategory.READER
            override val displayName = "Document reader"
            override val settings = settings
            override val sharedSettingDefinitions = mapOf(sharedSettingId to sharedDefinition)
        }

        val orphan = definition(ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE)
        assertThrows<IllegalArgumentException> {
            DefaultEntryViewerSettingsProvider(EntryType.BOOK, listOf(surface(emptyList(), orphan)))
        }.message shouldBe
            "Shared reader setting reader.shared must be included in surface book.document settings"

        val profileOnly = definition(ViewerSettingScope.PROFILE_ONLY)
        assertThrows<IllegalArgumentException> {
            DefaultEntryViewerSettingsProvider(EntryType.BOOK, listOf(surface(listOf(profileOnly), profileOnly)))
        }.message shouldBe "Shared reader setting reader.shared must support entry overrides"
    }
}
