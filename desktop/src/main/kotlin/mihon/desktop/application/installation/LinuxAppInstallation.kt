package mihon.desktop.application.installation

import mihon.desktop.application.DesktopAppVariant
import mihon.desktop.application.thisProcessLaunch
import java.io.File

/**
 * Linux: the app runs from an AppImage, whose runtime names it in `APPIMAGE`, or from the build tool. Only the release
 * updates itself, and only from an AppImage; a development build, or the app run from the build tool, has nothing a
 * release could replace.
 */
internal class LinuxAppInstallation : AppInstallation {
    private val appImage: String? = System.getenv("APPIMAGE")?.takeIf { it.isNotEmpty() }

    /**
     * The AppImage, where the app runs from one. The Java command inside it won't do: its files are mounted only while
     * the AppImage's first process lives, which ends as this one does.
     */
    override fun launch(): List<String>? = appImage?.let(::listOf) ?: thisProcessLaunch()

    override val update: InstallationUpdate? by lazy {
        appImage
            ?.let(::File)
            ?.takeIf {
                DesktopAppVariant.current == DesktopAppVariant.RELEASE &&
                    it.isFile &&
                    System.getProperty("os.arch") in setOf("amd64", "x86_64")
            }
            ?.let(::AppImageUpdate)
    }

    private class AppImageUpdate(private val appImage: File) : InstallationUpdate {
        override fun isPackageName(assetName: String): Boolean = RELEASE_APPIMAGE.matches(assetName)

        override fun checkReplaceable() = AppImageFiles.checkReplaceable(appImage)

        override fun isPackage(file: File): Boolean = AppImageFiles.isAppImage(file)

        override fun replace(file: File) = AppImageFiles.replace(appImage, file)
    }

    private companion object {
        /** The release's AppImage as the release workflow publishes it, `Katari-<version>-x86_64.AppImage`. */
        val RELEASE_APPIMAGE = Regex("""Katari-\d[^/]*-x86_64\.AppImage""")
    }
}
