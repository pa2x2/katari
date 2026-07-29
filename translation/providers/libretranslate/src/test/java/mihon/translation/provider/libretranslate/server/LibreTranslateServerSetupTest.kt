package mihon.translation.provider.libretranslate.server

import android.content.Context
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.translation.api.TranslationSetupDestination
import mihon.translation.spi.TranslationSetupResult
import org.junit.jupiter.api.Test

class LibreTranslateServerSetupTest {
    @Test
    fun `successful Katari setup launch is reported as in app`() = runTest {
        val setup = setup(opened = true)

        setup.openSetup() shouldBe
            TranslationSetupResult.Opened(TranslationSetupDestination.InApp)
    }

    @Test
    fun `failed Katari setup launch reports unavailable settings`() = runTest {
        val setup = setup(opened = false)

        setup.openSetup() shouldBe TranslationSetupResult.SettingsUnavailable
    }

    private fun setup(opened: Boolean) = LibreTranslateServerSetup(
        context = mockk<Context>(relaxed = true),
        configuration = mockk(relaxed = true),
        openInAppSetup = { opened },
    )
}
