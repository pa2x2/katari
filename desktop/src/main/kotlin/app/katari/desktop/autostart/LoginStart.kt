package app.katari.desktop.autostart

import app.katari.desktop.application.DesktopAppVariant
import app.katari.desktop.application.installation.AppInstallation
import app.katari.desktop.storage.DesktopDirectories
import app.katari.desktop.tray.TrayPreferences
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Starting the app at the user's login, hidden in the tray, through its [AutostartEntry] in the user's autostart
 * folder, where the installation has a program that outlasts this run ([AppInstallation.loginProgram]). The entry is
 * the setting: it is on while the app's own entry is there and the desktop's settings haven't turned it off, so it
 * follows what those settings change. Starting in the tray needs the tray, so turning
 * [TrayPreferences.keepRunningInTray] off takes the entry away.
 */
@SingleIn(AppScope::class)
@Inject
class LoginStart(
    directories: DesktopDirectories,
    installation: AppInstallation,
    private val trayPreferences: TrayPreferences,
) {
    private val program = installation.loginProgram
    private val file = directories.autostart.resolve("${DesktopAppVariant.current.applicationId}.desktop")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val changes = Mutex()
    private val on = MutableStateFlow(false)

    /** Whether the app can start at login from where it runs; Settings leaves the switch out where it can't. */
    val isOffered: Boolean = program != null

    /** Whether the desktop starts the app at login, as of the last look at its entry. */
    val isOn: StateFlow<Boolean> = on.asStateFlow()

    /**
     * Points an entry that is on at this run's program, which a moved AppImage left behind, and takes the entry away
     * whenever keeping the app running in the tray turns off.
     */
    fun start() {
        scope.launch {
            changes.withLock {
                reload()
                if (on.value) write()
            }
            trayPreferences.keepRunningInTray.changes().filter { !it }.collect { set(false) }
        }
    }

    /** Looks at the entry again, as the desktop's settings may have changed it since. */
    fun refresh() {
        scope.launch { changes.withLock { reload() } }
    }

    /** Writes the entry, or takes the app's own entry away. */
    fun set(start: Boolean) {
        scope.launch {
            changes.withLock {
                if (start) write() else remove()
                reload()
            }
        }
    }

    private fun reload() {
        on.value = program != null && file.isRegularFile() && read()?.let(AutostartEntry::isOwnAndEnabled) == true
    }

    private fun write() {
        val program = program ?: return
        val text = AutostartEntry.text(
            program = program,
            name = DesktopAppVariant.current.displayName,
            icon = DesktopAppVariant.current.applicationId,
        )
        if (read() == text) return
        try {
            Files.createDirectories(file.parent)
            // Beside the entry and moved over it, so the desktop never reads half an entry
            val next = file.resolveSibling(".${file.fileName}.next")
            next.writeText(text)
            Files.move(next, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "Couldn't write the login start entry $file" }
        }
    }

    private fun remove() {
        if (read()?.let(AutostartEntry::isOwn) != true) return
        try {
            file.deleteIfExists()
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "Couldn't remove the login start entry $file" }
        }
    }

    private fun read(): String? = try {
        if (file.isRegularFile()) file.readText() else null
    } catch (e: IOException) {
        logcat(LogPriority.WARN, e) { "Couldn't read the login start entry $file" }
        null
    }
}
