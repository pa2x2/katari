package eu.kanade.tachiyomi.extension.installer

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller.InstallRoute
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller.UserActionBehavior
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ExtensionInstallRoutingTest {

    @Test
    fun `private updates use private installation with operation user action behavior`() {
        val extension = installedExtension(isShared = false)

        ExtensionInstallOperation.ManualUpdate(extension).routing() shouldBe ExtensionInstallRouting(
            installRoute = InstallRoute.Private,
            userActionBehavior = UserActionBehavior.LaunchPrompt,
        )
        ExtensionInstallOperation.AutomaticUpdate(extension).routing() shouldBe ExtensionInstallRouting(
            installRoute = InstallRoute.Private,
            userActionBehavior = UserActionBehavior.MarkAsRequiresUserAction,
        )
    }

    @Test
    fun `shared updates use configured installation with operation user action behavior`() {
        val extension = installedExtension(isShared = true)

        ExtensionInstallOperation.ManualUpdate(extension).routing() shouldBe ExtensionInstallRouting(
            installRoute = InstallRoute.Configured,
            userActionBehavior = UserActionBehavior.LaunchPrompt,
        )
        ExtensionInstallOperation.AutomaticUpdate(extension).routing() shouldBe ExtensionInstallRouting(
            installRoute = InstallRoute.Configured,
            userActionBehavior = UserActionBehavior.MarkAsRequiresUserAction,
        )
    }

    @Test
    fun `new installs use configured installation and launch user action`() {
        ExtensionInstallOperation.NewInstall.routing() shouldBe ExtensionInstallRouting(
            installRoute = InstallRoute.Configured,
            userActionBehavior = UserActionBehavior.LaunchPrompt,
        )
    }

    private fun installedExtension(isShared: Boolean) = Extension.Installed(
        name = "Test extension",
        pkgName = "org.example.extension",
        versionName = "1.0.0",
        versionCode = 1L,
        libVersion = 1.6,
        lang = "en",
        isNsfw = false,
        pkgFactory = null,
        sources = emptyList(),
        icon = null,
        isShared = isShared,
    )
}
