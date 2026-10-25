package eu.kanade.tachiyomi.extension.kind

import eu.kanade.tachiyomi.extension.model.Extension

/** An extension built against a version of its [family]'s library that this build doesn't run. */
data class UnsupportedSdk(
    val family: ExtensionFamily,
    val libVersionName: String,
    /** Whether it's newer than every version this build runs, so only a newer app could; `null` when unreadable. */
    val isNewer: Boolean?,
)

/** Why this build can't run it, when that's its library's version. */
internal fun Extension.unsupportedSdk(): UnsupportedSdk? = when (this) {
    is Extension.NotLoaded -> (reason as? Extension.NotLoaded.Reason.UnsupportedLibVersion)?.unsupportedSdk()
    is Extension.Available -> ExtensionKinds.forListing(sourceIdScheme, libVersionName)
        ?.takeUnless { it.runsLib(libVersionName) }
        ?.unsupportedSdk(libVersionName)
    is Extension.Loaded -> null
}

internal fun Extension.NotLoaded.Reason.UnsupportedLibVersion.unsupportedSdk(): UnsupportedSdk {
    return ExtensionKinds.ofProvider(providerKind).unsupportedSdk(libVersionName)
}

private fun ExtensionKind.unsupportedSdk(libVersionName: String): UnsupportedSdk {
    return UnsupportedSdk(family, libVersionName, isNewerThanRun(libVersionName))
}
