package eu.kanade.tachiyomi.extension.platform

import mihon.core.common.platform.PlatformFacility
import java.io.File

/**
 * The apks the platform has: the packages installed for every app, the app's private copies in
 * [ExtensionFiles.privateApkDir], and a downloaded file. None of these tell extensions from other packages; the
 * extension kinds do.
 */
@PlatformFacility
interface ExtensionPackages {

    /** Every package installed for every app. */
    fun shared(): List<ExtensionPackage>

    /** The package installed for every app as [packageName], `null` when there's none. */
    fun shared(packageName: String): ExtensionPackage?

    /** Every private copy by its file, with `null` for one the platform can't read, such as a damaged file. */
    fun private(): Map<File, ExtensionPackage?>

    /** The private copy of [packageName], `null` when there's none or it can't be read. */
    fun private(packageName: String): ExtensionPackage?

    /** [file] read as an apk, `null` when it isn't one the platform can read. */
    fun downloaded(file: File): ExtensionPackage?
}
