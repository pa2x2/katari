package mihon.webview.jcef.runtime

import com.jetbrains.cef.JCefAppConfig
import com.jetbrains.cef.JCefVersionDetails
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.exists

sealed interface JcefPresence {
    /** [version] names JCEF and its Chromium, as "JCEF 150.0.14 (Chromium 150.0.7871.129)". */
    data class Present(val version: String) : JcefPresence

    /** The Java runtime carries no JCEF, as every runtime but the JetBrains Runtime with JCEF. */
    data object Absent : JcefPresence
}

/**
 * Whether the running JVM carries JCEF: the JetBrains Runtime's `jcef` module, and where the runtime keeps its own
 * native files, `sun.boot.library.path`, Chromium's server with Chromium beside it and the library JCEF reads the
 * server's pictures through. It loads nothing and starts nothing, so it can run before the window toolkit is chosen. It
 * names no JCEF class itself, as a runtime without JCEF couldn't load it.
 */
internal object JcefPresenceCheck {
    fun check(): JcefPresence {
        if (ModuleLayer.boot().findModule(JCEF_MODULE).isEmpty) return JcefPresence.Absent
        val libraryPath = System.getProperty(RUNTIME_LIBRARY_PATH).orEmpty().split(File.pathSeparator)
        val missing = NATIVE_FILES.filter { file -> libraryPath.none { directory -> Path(directory, file).exists() } }
        if (missing.isNotEmpty()) {
            logcat(LogPriority.WARN) { "The Java runtime has JCEF but not its native files $missing" }
            return JcefPresence.Absent
        }
        return JcefModuleVersion.read()?.let(JcefPresence::Present) ?: JcefPresence.Absent
    }
}

private const val JCEF_MODULE = "jcef"

private const val RUNTIME_LIBRARY_PATH = "sun.boot.library.path"

private val NATIVE_FILES = listOf(
    Chromium.SERVER_EXECUTABLE,
    System.mapLibraryName("cef"),
    // Shared memory between the app and the server, which carries the pictures of the pages
    System.mapLibraryName("shared_mem_helper"),
)

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
