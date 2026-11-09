package app.katari.desktop.application.classdata

import app.katari.core.metro.AppCoroutineScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.lang.management.ManagementFactory
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Keeps the archive of the app's classes that the JVM maps as the app starts, in place of loading and verifying them
 * from the jars, which is most of what a start does before the window shows. The AppImage's launcher has the JVM
 * record the classes of each run and map the archive once there is one ([ClassDataFolder]). A while into a run, once
 * its start has settled, the app collects what its runs recorded and builds the archive from it ([ArchiveBuild]), and
 * builds it again once later runs have loaded enough classes it lacks, such as those of screens the earlier runs never
 * opened. A build the app's end cuts short starts over in a later run. Outside an AppImage nothing records or maps
 * classes, and this does nothing.
 */
@Inject
@SingleIn(AppScope::class)
class ClassDataArchive(@AppCoroutineScope private val scope: CoroutineScope) {
    private val folder = System.getProperty(FOLDER_PROPERTY)?.let { ClassDataFolder(Path(it)) }
    private var upkeep: Job? = null

    @Volatile
    private var build: ArchiveBuild? = null

    fun start() {
        val folder = folder ?: return
        upkeep = scope.launch(Dispatchers.IO) {
            delay(START_DELAY)
            try {
                keepUp(folder)
            } catch (e: IOException) {
                logcat(LogPriority.WARN, e) { "Can't keep the class data archive" }
            }
        }
    }

    /** Stops a build under way: its JVM reads the app's jars from the AppImage, whose mount ends with the app. */
    fun stop() {
        upkeep?.cancel()
        build?.stop()
    }

    private suspend fun keepUp(folder: ClassDataFolder) {
        folder.dropOtherBuilds()
        // What a build an earlier run's end cut short had written
        folder.building.deleteIfExists()
        if (startedWithArchive() && !sharesClasses()) {
            logcat(LogPriority.WARN) { "The JVM refused the class data archive; building it again" }
            folder.dropArchive()
        }
        val collection = folder.collectSessions { processId ->
            ProcessHandle.of(processId).map(ProcessHandle::isAlive).orElse(false)
        }
        if (!folder.needsBuild(collection)) return
        val started = TimeSource.Monotonic.markNow()
        val build = ArchiveBuild.start(folder.collected, folder.building) ?: return
        this.build = build
        val built = try {
            build.await()
        } finally {
            build.stop()
            this.build = null
        }
        if (built) {
            folder.keepBuilt(collection)
            logcat { "Built the class data archive of ${collection.classCount} classes in ${started.elapsedNow()}" }
        } else {
            folder.building.deleteIfExists()
            logcat(LogPriority.WARN) { "The class data archive's build failed" }
        }
    }

    private fun startedWithArchive() =
        ManagementFactory.getRuntimeMXBean().inputArguments.any { it.startsWith(ARCHIVE_OPTION) }

    // The JVM tells it shares classes from an archive in its own description, and only then
    private fun sharesClasses() = SHARING in System.getProperty("java.vm.info").orEmpty()

    private companion object {
        // Set by the AppImage's launcher, which AssembleAppDirTask writes
        const val FOLDER_PROPERTY = "katari.classData"
        const val ARCHIVE_OPTION = "-XX:SharedArchiveFile="
        const val SHARING = "sharing"

        val START_DELAY = 20.seconds
    }
}
