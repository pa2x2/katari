package app.katari.desktop.logging

import app.katari.desktop.storage.DesktopDirectories
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * The app's log on disk, in the state directory: [current], which [FileLogger] appends to, and [previous], which the
 * current one becomes once it is full. Every run appends, so the log of a crashed run is still there when the crash
 * screen's run dumps it.
 */
@Inject
@SingleIn(AppScope::class)
class DesktopLogFiles(directories: DesktopDirectories) {
    private val directory: Path by lazy { Files.createDirectories(directories.state.resolve("logs")) }

    val current: Path
        get() = directory.resolve("katari.log")

    val previous: Path
        get() = directory.resolve("katari.previous.log")

    /** The files that exist, oldest first. */
    fun existing(): List<Path> = listOf(previous, current).filter { it.exists() }
}
