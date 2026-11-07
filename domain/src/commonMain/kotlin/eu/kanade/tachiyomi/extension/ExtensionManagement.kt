package eu.kanade.tachiyomi.extension

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import kotlinx.coroutines.flow.Flow

/**
 * The extensions the platform hosts and what the user does with them: the installed ones, loaded or not, what the
 * added stores list, and installing, updating, trusting and uninstalling them. The extension screens and the
 * missing-source sheet run on it.
 */
interface ExtensionManagement {
    /** False where the platform can't install extensions; entries then don't offer to install a missing source's. */
    val isInstallOffered: Boolean

    /** The installed extensions whose sources are registered, once the platform has looked for them. */
    val loaded: Flow<List<Extension.Loaded>>

    /** The installed extensions that weren't loaded, each with why, once the platform has looked for them. */
    val notLoaded: Flow<List<Extension.NotLoaded>>

    /**
     * What the added stores list, the newest listing of each extension per store signing key, or per store for ones
     * that don't sign their extensions.
     */
    val available: Flow<List<Extension.Available>>

    /** The step each install, update or reinstall that hasn't finished is at, by the package name it installs. */
    val installSteps: Flow<Map<String, InstallStep>>

    /** The installed extensions, loaded or not, once the platform has looked for them. */
    suspend fun installed(): List<Extension.Installed>

    /** The installed extension [pkgName], loaded or not; null when it isn't installed or not looked for yet. */
    fun installed(pkgName: String): Extension.Installed?

    /**
     * A store's listing of the extension that provides each source the added stores list, by the source's key, whether
     * or not it's installed. Empty until the stores' lists are first read; it follows the stores as they're added and
     * removed.
     */
    val listingsBySource: Flow<Map<Long, Extension.Available>>

    /** Reads again what the stores list, after reading the stores themselves again when [refreshStores]. */
    suspend fun refreshAvailable(refreshStores: Boolean = false)

    /** Reads what the stores list, unless it was read since the app started. */
    suspend fun fetchListingsOnce()

    /** Installs [listing], reporting each step until it completes. */
    fun install(listing: Extension.Available): Flow<InstallStep>

    /** Installs the newest listing that updates [extension]; completes at once when there's none. */
    fun update(extension: Extension.Installed): Flow<InstallStep>

    /** Installs the store's current file over a damaged one; the listing it came from, whatever its version. */
    fun reinstall(extension: Extension.Installed): Flow<InstallStep>

    fun cancelInstall(extension: Extension)

    fun uninstall(extension: Extension.Installed)

    /** Trusts an extension not loaded for being [Extension.NotLoaded.Reason.Untrusted], which then loads. */
    fun trust(extension: Extension.NotLoaded)

    /** Makes the extension [pkgName] provide the source under [sourceKey] instead of the one providing it now. */
    fun chooseSourceProvider(pkgName: String, sourceKey: Long)

    /**
     * Deletes the active profile's settings of every source of [extension], logins and saved data included, and has its
     * sources read them again where they can.
     */
    suspend fun clearData(extension: Extension.Loaded)
}
