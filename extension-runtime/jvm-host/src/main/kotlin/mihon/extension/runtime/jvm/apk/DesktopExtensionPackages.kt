package mihon.extension.runtime.jvm.apk

import eu.kanade.tachiyomi.extension.platform.ExtensionFiles
import eu.kanade.tachiyomi.extension.platform.ExtensionPackage
import eu.kanade.tachiyomi.extension.platform.ExtensionPackages
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.util.zip.ZipFile

/**
 * The app's private copies of apks, each a file named `<package>.apk` in [ExtensionFiles.privateApkDir]. Desktop has no
 * packages installed for every app.
 *
 * A file is read as Android reads an archive: one whose manifest can't be parsed, or whose signatures don't verify, isn't
 * a package it can read.
 */
class DesktopExtensionPackages(private val files: ExtensionFiles) : ExtensionPackages {

    override fun shared(): List<ExtensionPackage> = emptyList()

    override fun shared(packageName: String): ExtensionPackage? = null

    override fun private(): Map<File, ExtensionPackage?> {
        return files.privateApkDir
            .listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension == PRIVATE_APK_EXTENSION }
            .associateWith(::read)
    }

    override fun private(packageName: String): ExtensionPackage? {
        val file = privateApk(files, packageName)
        return if (file.isFile) read(file) else null
    }

    override fun downloaded(file: File): ExtensionPackage? = read(file)

    companion object {
        private const val PRIVATE_APK_EXTENSION = "apk"

        /** Where the private copy of [packageName] is kept. */
        fun privateApk(files: ExtensionFiles, packageName: String): File =
            File(files.privateApkDir, "$packageName.$PRIVATE_APK_EXTENSION")

        /** [apk] read as a package, `null` when it isn't one: not an apk, damaged, or signed with signatures that fail. */
        fun read(apk: File): DesktopExtensionPackage? {
            return try {
                val manifest = ZipFile(apk).use { zip ->
                    val entry = zip.getEntry(DesktopExtensionPackage.MANIFEST) ?: return null
                    ApkManifest.parse(zip.getInputStream(entry).readBytes())
                }
                val signatures = ApkSigners.of(apk)
                if (signatures == null) {
                    logcat(LogPriority.WARN) { "The signatures of ${apk.name} don't verify" }
                    return null
                }
                DesktopExtensionPackage(apk, manifest, signatures)
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Can't read ${apk.name} as an apk" }
                null
            }
        }
    }
}
