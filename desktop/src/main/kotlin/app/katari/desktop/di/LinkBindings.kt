package app.katari.desktop.di

import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.core.platform.link.LinkOpener
import app.katari.desktop.link.XdgLinkOpener
import app.katari.presentation.kit.feedback.AppMessages
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
object LinkBindings {
    @Provides
    fun linkOpener(
        operatingSystem: DesktopOperatingSystem,
        messages: AppMessages,
    ): LinkOpener = when (operatingSystem) {
        DesktopOperatingSystem.LINUX -> XdgLinkOpener(messages)
    }
}
