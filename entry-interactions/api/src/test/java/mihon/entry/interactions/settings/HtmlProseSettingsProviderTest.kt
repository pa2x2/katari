package mihon.entry.interactions.settings

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.ViewerSettingsCategory
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class HtmlProseSettingsProviderTest {
    private val provider = HtmlProseSettingsProvider(InMemoryPreferenceStore())

    @Test
    fun `provider exposes stable validated prose settings`() {
        provider.id shouldBe "builtin.book.prose.html"
        provider.category shouldBe ViewerSettingsCategory.READER
        provider.settings shouldHaveSize 10
        provider.settings.map { it.id.key }.toSet() shouldHaveSize 10
        provider.settings.all { it.id.providerId == provider.id } shouldBe true
        provider.settings.all { it.profilePreference.key().startsWith("book.prose.html.") } shouldBe true
    }

    @Test
    fun `pagination is the default and only layout supports entry override`() {
        provider.layoutModeSetting.processorDefault shouldBe HtmlProseSettingsProvider.LAYOUT_PAGINATED
        provider.layoutModeSetting.scope shouldBe ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE
        provider.settings
            .filterNot { it.id.key == HtmlProseSettingsProvider.LAYOUT_MODE_KEY }
            .all { it.scope == ViewerSettingScope.PROFILE_ONLY } shouldBe true
        provider.tapNavigationSetting.processorDefault shouldBe false
        provider.showProgressSetting.processorDefault shouldBe true
        provider.drawUnderCutoutSetting.processorDefault shouldBe false
    }

    @Test
    fun `numeric and choice values reject unsupported settings`() {
        provider.themeSetting.validate(HtmlProseSettingsProvider.THEME_BLACK) shouldBe true
        provider.themeSetting.validate("unknown") shouldBe false
        provider.fontSizeSetting.validate(69) shouldBe false
        provider.fontSizeSetting.validate(200) shouldBe true
        provider.lineHeightSetting.validate(221) shouldBe false
        provider.pageMarginsSetting.validate(-1) shouldBe false
    }
}
