package mihon.desktop.storage

import mihon.core.common.platform.DesktopOperatingSystem
import mihon.desktop.application.DesktopAppVariant

/** The [DesktopDirectories] of [operatingSystem], this build's own. */
fun systemDesktopDirectories(operatingSystem: DesktopOperatingSystem): DesktopDirectories {
    return when (operatingSystem) {
        DesktopOperatingSystem.LINUX -> XdgDesktopDirectories(DesktopAppVariant.current.directoryName)
    }
}
