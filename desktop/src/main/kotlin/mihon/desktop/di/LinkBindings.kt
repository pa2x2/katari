package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import mihon.core.common.platform.DesktopOperatingSystem
import mihon.core.platform.link.LinkOpener
import mihon.desktop.link.XdgLinkOpener
import mihon.presentation.kit.feedback.AppMessages

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
