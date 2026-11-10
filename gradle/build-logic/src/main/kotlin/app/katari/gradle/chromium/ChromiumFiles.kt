package app.katari.gradle.chromium

import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Chromium as the JetBrains Runtime's SDK carries it: the native files of its `jcef` module. The desktop app's AppImage
 * ships none of them but [KEPT_IN_RUNTIME]; the rest, less [LEFT_OUT], is the archive the app downloads Chromium from.
 */
internal object ChromiumFiles {
    const val MODULE = "jcef"

    /** What JCEF's Java side loads itself, to read the pictures of pages from the memory it shares with Chromium. */
    val KEPT_IN_RUNTIME = setOf("libshared_mem_helper.so")

    /**
     * What the app's Chromium never uses: the helper of the sandbox Chromium runs without under JCEF, and the library
     * through which JCEF would run Chromium inside the app's own process, where the app runs it in one of its own.
     */
    val LEFT_OUT = setOf("chrome-sandbox", "libjcef.so")

    /** The names directly in the module's native section, files and folders alike. */
    fun nativeEntries(sdkHome: File, execOperations: ExecOperations): Set<String> {
        val listing = ByteArrayOutputStream()
        execOperations.exec {
            commandLine(sdkHome.resolve("bin/jmod").absolutePath, "list", jmod(sdkHome).absolutePath)
            standardOutput = listing
        }
        return listing.toString().lineSequence()
            .mapNotNull { line -> line.removePrefix(NATIVE_SECTION).takeIf { it != line } }
            .map { it.substringBefore('/') }
            .toSet()
    }

    fun jmod(sdkHome: File): File = sdkHome.resolve("jmods/$MODULE.jmod")

    const val NATIVE_SECTION = "lib/"
}
