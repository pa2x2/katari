package eu.kanade.tachiyomi.extension.kind

/** An extension built against a version of its [family]'s library that this build doesn't run. */
data class UnsupportedSdk(
    val family: ExtensionFamilySupport,
    val libVersionName: String,
    /** Whether it's newer than every version this build runs, so only a newer app could; `null` when unreadable. */
    val isNewer: Boolean?,
)
