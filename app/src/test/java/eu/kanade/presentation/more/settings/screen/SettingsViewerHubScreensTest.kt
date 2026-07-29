package eu.kanade.presentation.more.settings.screen

import io.kotest.matchers.shouldBe
import mihon.entry.viewer.settings.ReaderSharedSettingAction
import mihon.entry.viewer.settings.ReaderSharedSettingAvailability
import mihon.entry.viewer.settings.ReaderSharedSettingText
import org.junit.jupiter.api.Test

class SettingsViewerHubScreensTest {

    @Test
    fun `disabled shared setting retains its reason and recovery action`() {
        val summary = ReaderSharedSettingText { "summary" }
        val reason = ReaderSharedSettingText { "reason" }
        val actionLabel = ReaderSharedSettingText { "configure" }
        val action = ReaderSharedSettingAction(actionLabel) {}

        val presentation = readerSharedSettingAppPresentation(
            availability = ReaderSharedSettingAvailability.Disabled(reason, action),
            summary = summary,
        )

        presentation.subtitle shouldBe listOf(reason, actionLabel)
        presentation.isVisible shouldBe true
        presentation.isInteractive shouldBe false
        presentation.disabledAction shouldBe action
    }

    @Test
    fun `shared setting becomes interactive only after availability is confirmed`() {
        val summary = ReaderSharedSettingText { "summary" }

        val loading = readerSharedSettingAppPresentation(null, summary)
        val available = readerSharedSettingAppPresentation(ReaderSharedSettingAvailability.Available, summary)

        loading.subtitle shouldBe listOf(summary)
        loading.isVisible shouldBe true
        loading.isInteractive shouldBe false
        available.subtitle shouldBe listOf(summary)
        available.isVisible shouldBe true
        available.isInteractive shouldBe true
    }
}
