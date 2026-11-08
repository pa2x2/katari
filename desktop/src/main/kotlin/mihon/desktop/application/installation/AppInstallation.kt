package mihon.desktop.application.installation

import mihon.core.common.platform.DesktopOperatingSystem
import java.io.File

/**
 * How this copy of the app is installed on the system: what starts it again, and whether and how it puts a release's
 * package in its own place.
 */
interface AppInstallation {
    /** The command that starts the app, without arguments; null where the system doesn't tell this process's. */
    fun launch(): List<String>?

    /** How the installation updates itself; null where it doesn't, as a development build or a build tool's run. */
    val update: InstallationUpdate?
}

/** Putting a release's package in the installed app's place, which runs once the app starts again. */
interface InstallationUpdate {
    /** Whether [assetName] is, among a release's files, the package this installation installs. */
    fun isPackageName(assetName: String): Boolean

    /**
     * Throws [mihon.feature.appupdate.AppUpdateException] where the installed app can't be replaced, checked before the
     * package, which is large, downloads.
     */
    fun checkReplaceable()

    /** Whether the downloaded [file] is a package this installation can be replaced with. */
    fun isPackage(file: File): Boolean

    /**
     * Puts [file] in the installed app's place. Throws [mihon.feature.appupdate.AppUpdateException] where it can't.
     */
    fun replace(file: File)
}

fun systemAppInstallation(operatingSystem: DesktopOperatingSystem): AppInstallation = when (operatingSystem) {
    DesktopOperatingSystem.LINUX -> LinuxAppInstallation()
}
