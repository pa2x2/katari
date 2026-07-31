package mihon.translation.provider.libretranslate.offline

import android.content.Context
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.translation.api.host.TranslationSetupDestination
import mihon.translation.spi.setup.TranslationSetupResult
import org.junit.jupiter.api.Test

class OfflineTranslatorSetupTest {
    @Test
    fun `installed provider opens Katari setup in app`() = runTest {
        val application = FakeOfflineTranslatorApp(installed = true)
        var inAppLaunches = 0
        val setup = setup(application) {
            inAppLaunches += 1
            true
        }

        setup.openSetup() shouldBe
            TranslationSetupResult.Opened(TranslationSetupDestination.InApp)
        inAppLaunches shouldBe 1
        application.installationPageLaunches shouldBe 0
    }

    @Test
    fun `missing provider opens its installation page externally`() = runTest {
        val application = FakeOfflineTranslatorApp(
            installed = false,
            installationPageOpened = true,
        )
        var inAppLaunches = 0
        val setup = setup(application) {
            inAppLaunches += 1
            true
        }

        setup.openSetup() shouldBe
            TranslationSetupResult.Opened(TranslationSetupDestination.External)
        inAppLaunches shouldBe 0
        application.installationPageLaunches shouldBe 1
    }

    @Test
    fun `failed in app and external launches report unavailable settings`() = runTest {
        val installedApplication = FakeOfflineTranslatorApp(installed = true)
        setup(installedApplication) { false }.openSetup() shouldBe
            TranslationSetupResult.SettingsUnavailable

        val missingApplication = FakeOfflineTranslatorApp(
            installed = false,
            installationPageOpened = false,
        )
        setup(missingApplication) { true }.openSetup() shouldBe
            TranslationSetupResult.SettingsUnavailable
    }

    private fun setup(
        application: OfflineTranslatorApp,
        openInAppSetup: () -> Boolean,
    ) = OfflineTranslatorSetup(
        context = mockk<Context>(relaxed = true),
        application = application,
        settings = mockk(relaxed = true),
        openInAppSetup = openInAppSetup,
    )

    private class FakeOfflineTranslatorApp(
        private val installed: Boolean,
        private val installationPageOpened: Boolean = false,
    ) : OfflineTranslatorApp {
        var installationPageLaunches = 0

        override fun isInstalled() = installed

        override fun open() = false

        override fun openInstallationPage(): Boolean {
            installationPageLaunches += 1
            return installationPageOpened
        }
    }
}
