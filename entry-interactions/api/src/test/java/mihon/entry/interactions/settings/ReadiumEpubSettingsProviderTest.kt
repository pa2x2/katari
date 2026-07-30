package mihon.entry.interactions.settings

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.ViewerSettingsCategory
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class ReadiumEpubSettingsProviderTest {

    private val provider = ReadiumEpubSettingsProvider(InMemoryPreferenceStore())

    @Test
    fun `provider has stable reader identity and unique settings`() {
        provider.id shouldBe "builtin.book.epub.readium"
        provider.category shouldBe ViewerSettingsCategory.READER
        provider.settings shouldHaveSize 11
        provider.settings.map { it.id.key }.toSet() shouldHaveSize 11
        provider.settings.all { it.id.providerId == provider.id } shouldBe true
    }

    @Test
    fun `reader settings are profile scoped`() {
        provider.settings.all { it.scope == ViewerSettingScope.PROFILE_ONLY } shouldBe true
    }

    @Test
    fun `portable values use namespaced profile keys and validated bounds`() {
        provider.settings.all {
            it.profilePreference.key().startsWith("book.epub.readium.")
        } shouldBe true

        provider.themeSetting.validate(ReadiumEpubSettingsProvider.THEME_SEPIA) shouldBe true
        provider.themeSetting.validate("unknown") shouldBe false
        provider.fontSizeSetting.validate(49) shouldBe false
        provider.fontSizeSetting.validate(50) shouldBe true
        provider.fontSizeSetting.validate(300) shouldBe true
        provider.fontSizeSetting.validate(301) shouldBe false
        provider.lineHeightSetting.validate(99) shouldBe false
        provider.lineHeightSetting.validate(200) shouldBe true
        provider.pageMarginsSetting.validate(-1) shouldBe false
        provider.pageMarginsSetting.validate(400) shouldBe true
        provider.tapNavigationSetting.processorDefault shouldBe false
        provider.showPageNumberSetting.processorDefault shouldBe true
    }
}
