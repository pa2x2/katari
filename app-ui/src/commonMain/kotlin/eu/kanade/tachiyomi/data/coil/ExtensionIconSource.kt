package eu.kanade.tachiyomi.data.coil

import coil3.fetch.FetchResult
import eu.kanade.tachiyomi.extension.platform.ExtensionPackage
import mihon.core.common.platform.PlatformFacility
import java.io.File

/** How the platform draws the icons of installed extensions, for [ExtensionIconFetcher]. */
@PlatformFacility
interface ExtensionIconSource {
    /** The launcher icon [extensionPackage] draws, at [density]. Throws when it has none the platform can draw. */
    fun packageIcon(extensionPackage: ExtensionPackage, density: ExtensionIconDensity): FetchResult

    /** The icon [file], which a file extension's store listed. Throws when it doesn't decode. */
    fun fileIcon(file: File): FetchResult
}

/** Which of an apk's icon resources to draw. */
enum class ExtensionIconDensity {
    /** The one the system picks for its screen. */
    System,

    /** The baseline one, as a row draws it. */
    Baseline,

    /** The sharpest, for a large header. */
    Sharpest,
}
