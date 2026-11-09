package app.katari.desktop.application.classdata

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * The class data the app keeps for one build of itself, in the folder the AppImage's launcher names after that build
 * and lays out as `AssembleAppDirTask` describes: the JVM records the classes of each run in [sessions], named after
 * its process, and maps [archive] once there is one. What the runs recorded is collected in [collected], and the
 * archive is built from [archived], the collection as it stood then.
 */
internal class ClassDataFolder(private val folder: Path) {
    val archive: Path = folder.resolve("archive.jsa")

    /** Where a build writes the archive, which takes the place of [archive] once it is whole. */
    val building: Path = folder.resolve("building.jsa")

    val collected: Path = folder.resolve("collected.classlist")
    private val archived = folder.resolve("archive.classlist")
    private val sessions = folder.resolve("sessions")

    /** Deletes the class data of the app's other builds, which their launchers alone pass the JVM. */
    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    fun dropOtherBuilds() {
        folder.parent.listDirectoryEntries().filter { it.name != folder.name }.forEach { it.deleteRecursively() }
    }

    fun dropArchive() {
        archive.deleteIfExists()
        archived.deleteIfExists()
    }

    /**
     * Adds what every run recorded to [collected] and returns the whole collection. A run that has ended has its
     * record deleted once it is added; one still running, this one among them, is added as it stands, and whole once
     * it has ended.
     */
    fun collectSessions(isRunning: (processId: Long) -> Boolean): ClassList {
        var collection = read(collected)
        val ended = mutableListOf<Path>()
        sessions.listDirectoryEntries("*$CLASS_LIST").forEach { record ->
            collection += ClassList.read(record)
            val processId = record.name.removePrefix(PROCESS_PREFIX).removeSuffix(CLASS_LIST).toLongOrNull()
            if (processId == null || !isRunning(processId)) ended.add(record)
        }
        replace(collected) { collection.write(it) }
        ended.forEach { it.deleteIfExists() }
        return collection
    }

    /**
     * Whether the archive is worth building from [collection]: there is none, or the runs since it was built loaded
     * enough classes it lacks to repay a build, which keeps a core busy for several seconds.
     */
    fun needsBuild(collection: ClassList): Boolean =
        !archive.exists() || collection.classCount - read(archived).classCount >= CLASSES_WORTH_A_BUILD

    /**
     * Puts the archive just built in [building] in place, as built from [collection]. An app ended in between leaves
     * the archive counted as built from less than it was, which costs at most a build sooner.
     */
    fun keepBuilt(collection: ClassList) {
        Files.move(building, archive, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        replace(archived) { collection.write(it) }
    }

    private fun read(list: Path): ClassList = if (list.exists()) ClassList.read(list) else ClassList.EMPTY

    // Written beside it first, so an app ended midway leaves the file as it was
    private fun replace(file: Path, write: (Path) -> Unit) {
        val partial = file.resolveSibling("${file.name}.partial")
        write(partial)
        Files.move(partial, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private companion object {
        // The JVM names each run's record after its process, from the launcher's `%p`
        const val PROCESS_PREFIX = "pid"
        const val CLASS_LIST = ".classlist"

        const val CLASSES_WORTH_A_BUILD = 300
    }
}
