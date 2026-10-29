package mihon.desktop.extension

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.platform.ExtensionPackage
import eu.kanade.tachiyomi.extension.platform.ExtensionPackages
import java.io.File

/**
 * Desktop reads no apks yet, so it has none installed and a downloaded one isn't read. Stands in until desktop loads
 * apk extensions.
 */
@Inject
class NoExtensionPackages : ExtensionPackages {
    override fun shared(): List<ExtensionPackage> = emptyList()

    override fun shared(packageName: String): ExtensionPackage? = null

    override fun private(): Map<File, ExtensionPackage?> = emptyMap()

    override fun private(packageName: String): ExtensionPackage? = null

    override fun downloaded(file: File): ExtensionPackage? = null
}
