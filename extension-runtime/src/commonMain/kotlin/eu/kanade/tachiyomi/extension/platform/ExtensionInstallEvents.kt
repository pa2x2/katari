package eu.kanade.tachiyomi.extension.platform

/**
 * The extensions added, replaced or removed: the platform's packages, and the app's own private copies and file
 * extensions, which it announces here itself.
 */
interface ExtensionInstallEvents {

    /** Starts handing every event to [onEvent], in the order they happen. Called once. */
    fun register(onEvent: (ExtensionInstallEvent) -> Unit)

    fun announceAdded(pkgName: String)

    fun announceReplaced(pkgName: String)

    fun announceRemoved(pkgName: String)
}

sealed interface ExtensionInstallEvent {
    val pkgName: String

    data class Added(override val pkgName: String) : ExtensionInstallEvent

    data class Replaced(override val pkgName: String) : ExtensionInstallEvent

    data class Removed(override val pkgName: String) : ExtensionInstallEvent
}
