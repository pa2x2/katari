package mihon.video.engine.mpv.addon

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PatternMatcher
import androidx.core.content.ContextCompat
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mihon.video.engine.mpv.loading.MpvLibraryLoader
import mihon.video.engine.mpv.loading.MpvLoadOutcome

/**
 * The mpv add-on on this device: whether it is installed and usable by this Katari, kept current from package events,
 * and the one load of its libraries per process.
 */
@Inject
@SingleIn(AppScope::class)
class MpvAddon(private val context: Application) {

    val requirement: MpvAddonRequirement by lazy { MpvAddonRequirement.of(context) }

    private val mutableState = MutableStateFlow<MpvAddonState>(MpvAddonState.Unknown)
    val state: StateFlow<MpvAddonState> = mutableState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val readLock = Mutex()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refresh()
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
            addDataSchemeSpecificPart(MpvAddonPackage.NAME, PatternMatcher.PATTERN_LITERAL)
        }
        ContextCompat.registerReceiver(context, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        refresh()
    }

    /** Reads the package again, for callers that may have missed an event, such as a screen coming back. */
    fun refresh() {
        scope.launch { read() }
    }

    /** The current state, read now. */
    suspend fun read(): MpvAddonState = readLock.withLock {
        withContext(Dispatchers.IO) {
            val loaded = MpvLibraryLoader.loaded
            val state = InstalledMpvAddon.read(context, requirement)?.let { addon ->
                MpvAddonState.Installed(
                    versionName = addon.versionName,
                    versionCode = addon.versionCode,
                    glue = addon.glue,
                    sizeBytes = addon.sizeBytes,
                    problem = addon.problem(requirement),
                    restartNeeded = loaded != null && loaded.versionCode != addon.versionCode,
                )
            } ?: MpvAddonState.NotInstalled
            mutableState.value = state
            state
        }
    }

    /**
     * mpv can play in this process: its libraries are loaded already, which survives the add-on's later update or
     * removal, or a usable add-on is installed and no load has failed.
     */
    val isAvailable: Boolean
        get() = when (MpvLibraryLoader.outcome) {
            is MpvLoadOutcome.Loaded -> true
            is MpvLoadOutcome.Failed -> false
            null -> (state.value as? MpvAddonState.Installed)?.problem == null && state.value is MpvAddonState.Installed
        }

    /** Whether this process has loaded the add-on's libraries, which an update can't replace before a restart. */
    val isLoaded: Boolean
        get() = MpvLibraryLoader.loaded != null

    /**
     * Loads the add-on's libraries unless already loaded, and returns false when mpv can't be used in this process.
     * Must succeed before the first native call into `MPVLib`. Blocks; call it off the main thread.
     */
    internal fun loadNow(): Boolean {
        MpvLibraryLoader.outcome?.let { return it is MpvLoadOutcome.Loaded }
        val addon = InstalledMpvAddon.read(context, requirement)
        val libraries = addon?.libraries
        val outcome = if (addon == null || libraries == null || addon.problem(requirement) != null) {
            null
        } else {
            MpvLibraryLoader.load(addon.versionCode, libraries)
        }
        refresh()
        return outcome is MpvLoadOutcome.Loaded
    }
}
