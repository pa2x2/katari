package app.katari.extension.runtime.jvm.icons

import app.katari.extension.runtime.jvm.apk.DesktopExtensionPackage
import eu.kanade.tachiyomi.extension.platform.ExtensionPackage
import java.util.zip.ZipFile

/** The launcher icon an installed apk draws, read from its resources in its sharpest bitmap. */
object ApkIcons {

    /** The icon's image file bytes, `null` for a package without a bitmap icon, such as one with only an XML one. */
    fun iconOf(extensionPackage: ExtensionPackage): ByteArray? {
        val apk = extensionPackage as? DesktopExtensionPackage ?: return null
        val iconId = apk.manifest.iconResourceId.takeIf { it != 0 } ?: return null
        val path = apk.resources?.bitmapFile(iconId) ?: return null
        return ZipFile(apk.apk).use { zip -> zip.getEntry(path)?.let { zip.getInputStream(it).readBytes() } }
    }
}
