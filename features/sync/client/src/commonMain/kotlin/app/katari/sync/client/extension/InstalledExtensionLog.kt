package app.katari.sync.client.extension

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.katari.sync.client.kind.extension.InstalledExtensionKind
import app.katari.sync.client.preference.SyncPreferences
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.extension.ExtensionManagement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.data.DatabaseHandler
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Keeps this device's installs and uninstalls of extensions in `extension_installs`, which [InstalledExtensionKind]
 * syncs. Extensions come and go outside the app too, so it compares what is installed with what it last kept, when
 * the app starts and whenever the installed extensions change; an extension untrusted here counts as installed.
 *
 * It keeps them whether or not the device syncs, so a first sync finds them as they are. The extensions found the
 * first time it runs were installed at a time it doesn't know, and are kept as such.
 */
@Inject
@SingleIn(AppScope::class)
class InstalledExtensionLog(
    private val handler: DatabaseHandler,
    private val extensions: ExtensionManagement,
    private val preferences: SyncPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var following: Job? = null

    /** Starts following the installed extensions, unless it already does. */
    fun start() {
        synchronized(this) {
            if (following == null) following = scope.launch { follow() }
        }
    }

    private suspend fun follow() {
        combine(extensions.loaded, extensions.notLoaded) { loaded, notLoaded ->
            (loaded.map { it.pkgName } + notLoaded.map { it.pkgName }).toSet()
        }
            .distinctUntilChanged()
            .collectLatest { installed ->
                // An update replaces an extension in steps, which would pass for an uninstall and an install
                delay(SETTLE)
                withContext(NonCancellable) { record(installed) }
            }
    }

    internal suspend fun record(installed: Set<String>) {
        val known = preferences.installedExtensionsKept.get()
        val now = Clock.System.now().toEpochMilliseconds()
        handler.await(inTransaction = true) {
            val kept = extension_installsQueries.all().awaitAsList().associate { it.pkg_name to it.installed }
            installed.filter { kept[it] != true }.forEach {
                extension_installsQueries.set(it, installed = true, changedAt = if (known) now else 0L)
            }
            kept.filter { (pkgName, isInstalled) -> isInstalled && pkgName !in installed }.keys.forEach {
                extension_installsQueries.set(it, installed = false, changedAt = now)
            }
        }
        if (!known) preferences.installedExtensionsKept.set(true)
    }

    private companion object {
        val SETTLE = 2.seconds
    }
}
