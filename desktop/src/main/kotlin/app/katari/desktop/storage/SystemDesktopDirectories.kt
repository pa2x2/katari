package app.katari.desktop.storage

import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.desktop.application.DesktopAppVariant

/** The [DesktopDirectories] of [operatingSystem], this build's own. */
fun systemDesktopDirectories(operatingSystem: DesktopOperatingSystem): DesktopDirectories {
    return when (operatingSystem) {
        DesktopOperatingSystem.LINUX -> XdgDesktopDirectories(DesktopAppVariant.current.directoryName)
    }
}
