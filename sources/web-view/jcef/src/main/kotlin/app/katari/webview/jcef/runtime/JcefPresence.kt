package app.katari.webview.jcef.runtime

import com.jetbrains.cef.JCefAppConfig
import com.jetbrains.cef.JCefVersionDetails
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

sealed interface JcefPresence {
    /**
     * JCEF with a Chromium to run. [version] names both, as "JCEF 150.0.14 (Chromium 150.0.7871.129)"; [chromium] is
     * the folder of a Chromium installed for the app, or null for the Java runtime's own.
     */
    data class Present(val version: String, val chromium: Path?) : JcefPresence

    /** The Java runtime carries JCEF but no Chromium, as the released app's, and none is installed for the app. */
    data object NoChromium : JcefPresence

    /** The Java runtime carries no JCEF, as every runtime but the JetBrains Runtime with JCEF. */
    data object Absent : JcefPresence
}

/**
 * Whether the running JVM carries JCEF and has a Chromium for it: the JetBrains Runtime's `jcef` module with the
 * library it reads Chromium's pictures through, and Chromium itself either where the runtime keeps its own native
 * files, `sun.boot.library.path`, or in the folder the app installs one to. It loads nothing and starts nothing, so it
 * can run before the window toolkit is chosen. It names no JCEF class itself, as a runtime without JCEF couldn't load it.
 */
internal class JcefPresenceCheck(private val installed: Path) {
    private val libraryPath = System.getProperty(RUNTIME_LIBRARY_PATH).orEmpty().split(File.pathSeparator).map(::Path)

    /** The Java runtime's own Chromium, which a runtime either has for the whole run or doesn't. */
    val hasRuntimeChromium: Boolean = libraryPath.any(::holdsChromium)

    /** The runtime's library for the memory Chromium's server hands the pages' pictures over in, if it has one. */
    val sharedMemoryLibrary: Path? =
        libraryPath.map { it.resolve(SHARED_MEMORY_LIBRARY) }.firstOrNull { it.exists() }

    private val version by lazy { JcefModuleVersion.read() }

    fun check(): JcefPresence {
        if (ModuleLayer.boot().findModule(JCEF_MODULE).isEmpty) return JcefPresence.Absent
        if (sharedMemoryLibrary == null) {
            logcat(LogPriority.WARN) { "The Java runtime has JCEF but not its $SHARED_MEMORY_LIBRARY" }
            return JcefPresence.Absent
        }
        val version = version ?: return JcefPresence.Absent
        return when {
            hasRuntimeChromium -> JcefPresence.Present(version, chromium = null)
            holdsChromium(installed) -> JcefPresence.Present(version, installed)
            else -> JcefPresence.NoChromium
        }
    }

    private fun holdsChromium(directory: Path) = CHROMIUM_FILES.all { directory.resolve(it).exists() }

    private companion object {
        const val JCEF_MODULE = "jcef"
        const val RUNTIME_LIBRARY_PATH = "sun.boot.library.path"

        // Shared memory between the app and the server, which carries the pictures of the pages
        val SHARED_MEMORY_LIBRARY: String = System.mapLibraryName("shared_mem_helper")

        val CHROMIUM_FILES = listOf(Chromium.SERVER_EXECUTABLE, System.mapLibraryName("cef"))
    }
}

/** The version JCEF's module records, read only once the module is known to be there. */
private object JcefModuleVersion {
    fun read(): String? {
        val details = try {
            JCefAppConfig.getVersionDetails()
        } catch (e: JCefVersionDetails.VersionUnavailableException) {
            logcat(LogPriority.WARN, e) { "The Java runtime's JCEF doesn't tell its version" }
            return null
        }
        val cef = details.cefVersion
        val chromium = details.chromiumVersion
        return "JCEF ${cef.major}.${cef.api}.${cef.patch} " +
            "(Chromium ${chromium.major}.${chromium.minor}.${chromium.build}.${chromium.patch})"
    }
}
