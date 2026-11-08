package mihon.video.engine.mpv.loading

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.File

/**
 * Loads the add-on's native libraries into Katari's linker namespace, once per process. `MPVLib`'s natives bind to
 * them on first use, through Katari's class loader.
 *
 * The libraries are loaded by absolute path, dependencies first, because the linker resolves their DT_NEEDED entries
 * only by soname among libraries already loaded. A failure after the first `System.load` is final for the process: a
 * retry would load into a half-populated namespace. A second load of other files (a newer add-on) would map a second
 * copy that `MPVLib`, already bound to the first, never uses, so the add-on only changes with a restart.
 */
internal object MpvLibraryLoader {
    private val lock = Any()

    /** Null until the first load; written under [lock]. */
    @Volatile
    var outcome: MpvLoadOutcome? = null
        private set

    val loaded: MpvLoadOutcome.Loaded?
        get() = outcome as? MpvLoadOutcome.Loaded

    /** Blocks while loading; call it off the main thread. */
    fun load(versionCode: Long, libraries: List<File>): MpvLoadOutcome = synchronized(lock) {
        outcome?.let { return it }
        val result = try {
            libraries.forEach { System.load(it.absolutePath) }
            MpvLoadOutcome.Loaded(versionCode)
        } catch (e: UnsatisfiedLinkError) {
            logcat(LogPriority.ERROR, e) { "Loading the mpv add-on $versionCode failed" }
            MpvLoadOutcome.Failed(versionCode, e.message)
        } catch (e: SecurityException) {
            logcat(LogPriority.ERROR, e) { "Loading the mpv add-on $versionCode was refused" }
            MpvLoadOutcome.Failed(versionCode, e.message)
        }
        outcome = result
        result
    }
}

internal sealed interface MpvLoadOutcome {
    val versionCode: Long

    data class Loaded(override val versionCode: Long) : MpvLoadOutcome

    data class Failed(override val versionCode: Long, val message: String?) : MpvLoadOutcome
}
