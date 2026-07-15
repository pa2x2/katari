package eu.kanade.presentation.more.settings.screen

import io.kotest.matchers.shouldBe
import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import org.junit.jupiter.api.Test

class SettingsViewerHubScreensTest {
    @Test
    fun `web prose provider has a reader settings destination`() {
        viewerSettingsScreen(HtmlProseSettingsProvider.PROVIDER_ID) shouldBe SettingsHtmlProseReaderScreen
    }
}
