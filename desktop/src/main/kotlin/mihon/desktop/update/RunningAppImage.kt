package mihon.desktop.update

import mihon.desktop.application.DesktopAppVariant
import java.io.File

/**
 * The AppImage the app runs from, which an update replaces: its runtime names it in `APPIMAGE`. Only the release
 * updates itself, and only from an AppImage; a development build, or the app run from the build tool, has nothing a
 * release could replace.
 */
internal object RunningAppImage {
    val file: File? by lazy {
        if (DesktopAppVariant.current != DesktopAppVariant.RELEASE) return@lazy null
        System.getenv("APPIMAGE")?.takeIf { it.isNotEmpty() }?.let(::File)?.takeIf(File::isFile)
    }

    val canUpdate: Boolean get() = file != null && System.getProperty("os.arch") in setOf("amd64", "x86_64")
}
