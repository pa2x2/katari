package app.katari.extension.runtime.jvm.conversion

import app.katari.extension.runtime.jvm.apk.DesktopExtensionPackage
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.time.measureTime

/**
 * The jars apks were converted to, in [root]: `<package>/<apk SHA-256>-<converter version>.jar`, so a changed apk or a
 * new converter gets a new jar and an unchanged one is converted once. A jar is written to a temporary file and moved
 * into place, so a conversion cut short leaves nothing that looks finished; converting a package's jar removes the
 * package's older ones. The folder is a cache: anything deleted from it is converted again when next needed.
 */
class ConvertedJarCache(private val root: Path) {

    private val locks = ConcurrentHashMap<Path, Any>()

    /** The jar of [extensionPackage], converted first when it has none yet. Throws [DexConversionException]. */
    fun jarOf(extensionPackage: DesktopExtensionPackage): Path {
        val directory = packageDirectory(extensionPackage.packageName)
        val jar = directory.resolve("${extensionPackage.sha256}-${DexConversion.converterVersion}.jar")
        if (jar.isRegularFile()) return jar
        synchronized(locks.computeIfAbsent(jar) { Any() }) {
            if (jar.isRegularFile()) return jar
            directory.createDirectories()
            val partial = directory.resolve("${jar.fileName}.part")
            partial.deleteIfExists()
            try {
                val took = measureTime { DexConversion.convert(extensionPackage.apk, partial) }
                logcat(LogPriority.INFO) {
                    "Converted ${extensionPackage.packageName} in ${took.inWholeMilliseconds} ms"
                }
                // The temporary file is in the same folder, so the move is a rename
                Files.move(partial, jar, StandardCopyOption.ATOMIC_MOVE)
            } finally {
                partial.deleteIfExists()
            }
            directory.listDirectoryEntries()
                .filter { it != jar && !it.fileName.toString().endsWith(".part") }
                .forEach { it.deleteIfExists() }
            return jar
        }
    }

    /** Removes every jar of [packageName], once it's uninstalled. */
    fun remove(packageName: String) {
        val directory = packageDirectory(packageName)
        if (!Files.isDirectory(directory)) return
        directory.listDirectoryEntries().forEach { it.deleteIfExists() }
        directory.deleteIfExists()
    }

    private fun packageDirectory(packageName: String): Path {
        // A package name is a folder name here, so it's held to what Android allows in one
        require(PACKAGE_NAME.matches(packageName)) { "Not a package name: $packageName" }
        return root.resolve(packageName)
    }

    private companion object {
        val PACKAGE_NAME = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
    }
}
