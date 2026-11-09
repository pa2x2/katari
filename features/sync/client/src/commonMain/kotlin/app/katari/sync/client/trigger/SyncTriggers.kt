package app.katari.sync.client.trigger

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.core.platform.network.NetworkConnectivity
import app.katari.profiles.core.ProfileStore
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.handoff.EntryHandOff
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.run.SyncRequest
import app.katari.sync.client.run.SyncRuns
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import tachiyomi.data.DatabaseHandler
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * When sync runs (D021), on every platform; the platforms call in when the app shows or hides and when it comes to the
 * front or leaves it, and the readers and the player when a period of reading or watching was recorded. A desktop
 * window can show without being in front, open on another monitor or behind other windows; on Android the two go
 * together.
 *
 * - A local edit: a run [QUIET] after the last edit of a burst, or [MAX_WAIT] after its first, whichever comes first,
 *   so marking 200 chapters read is one push. Reading position counts as an edit, so it goes 5 seconds after the last
 *   page turn.
 * - Leaving or pausing a reader or the player, and the app leaving the front (screen off, another app): the
 *   platform's hand-off work ([SyncBackgroundWork.handOff]), which finishes in the background and retries until the
 *   server has it.
 * - The app coming to the front: a run that fetches the entry Resume continues first (D052), then pulls and pushes.
 * - While shown: the live channel ([LiveUpdates]).
 * - In the background: the platform's periodic work while background sync is on, at the interval the person chose.
 * - Syncing resumed, or Wi-Fi reached while changes wait for it: a run, for what waited.
 * - A long run, moving the whole library in a first sync or a full resync: the platform's work for it, which shows its
 *   progress and goes on when the app leaves the front.
 *
 * None of it runs while the device isn't linked to an account (D001); while syncing is paused, runs end at once.
 */
@Inject
@SingleIn(AppScope::class)
class SyncTriggers(
    private val runs: SyncRuns,
    private val live: LiveUpdates,
    private val localChanges: LocalChanges,
    private val work: SyncBackgroundWork,
    private val connection: SyncConnection,
    private val preferences: SyncPreferences,
    private val handler: DatabaseHandler,
    private val profileStore: ProfileStore,
    private val handOff: EntryHandOff,
    private val network: NetworkConnectivity,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val shown = MutableStateFlow(false)
    private var started = false

    /** Starts the triggers that watch by themselves, once a process, after capture has resumed. */
    fun start() {
        synchronized(this) {
            if (started) return
            started = true
        }
        val linked = connection.states().map { it.isLinked }.distinctUntilChanged()
        scope.launch { live.wanted(shown).collectLatest { wanted -> if (wanted) live.listen() } }
        scope.launch { linked.collectLatest { isLinked -> if (isLinked) sendLocalEdits() } }
        scope.launch {
            connection.states().map { state -> state.stage.takeIf { state.isLinked && it.movesLibrary } }
                .distinctUntilChanged { old, new -> (old == null) == (new == null) }
                .collect { stage ->
                    // Only a first download waits for Wi-Fi (D022); a full resync goes on any network
                    if (stage != null) {
                        work.longRun(onMetered = preferences.useMobileData.get() || !stage.isFirstDownload)
                    }
                }
        }
        scope.launch { linked.collect { isLinked -> if (!isLinked) work.dropLongRun() } }
        scope.launch {
            combine(
                linked,
                preferences.syncInBackground.changes(),
                preferences.paused.changes(),
                preferences.backgroundInterval.changes(),
            ) { isLinked, inBackground, paused, minutes ->
                (isLinked && inBackground && !paused) to minutes.minutes
            }
                .distinctUntilChanged()
                .collect { (enabled, every) -> work.schedulePeriodic(enabled, every) }
        }
        // What waited for syncing to resume, or for Wi-Fi, goes as soon as it may
        scope.launch {
            combine(linked, preferences.paused.changes(), changesWaitForWifi()) { isLinked, paused, waits ->
                isLinked && !paused && !waits
            }
                .distinctUntilChanged()
                .drop(1)
                .collect { mayGo -> if (mayGo) runs.request(SyncRequest()) }
        }
    }

    private fun changesWaitForWifi() = combine(
        preferences.changesOnMobileData.changes(),
        network.state(),
    ) { onMobileData, network -> !onMobileData && !network.countsAsWifi }.distinctUntilChanged()

    fun appShown() {
        shown.value = true
    }

    fun appHidden() {
        shown.value = false
    }

    fun appCameToFront() {
        scope.launch {
            if (!connection.state().isLinked) return@launch
            runs.request(SyncRequest(entriesFirst = setOfNotNull(entryResumeContinues())))
        }
    }

    fun appWentToBack() {
        handOff()
    }

    /** A reader or the player recorded a period of reading or watching: on leaving, pausing, or a checkpoint. */
    fun readingRecorded() {
        handOff()
    }

    /** Fetches [entryId]'s changes ahead of everything else, as when it is opened (D052). */
    fun entryOpened(entryId: Long) {
        scope.launch {
            if (!connection.state().isLinked) return@launch
            // Checking from now, while a run already going finishes before the one that fetches it
            handOff.checking(setOf(entryId))
            runs.request(SyncRequest(entriesFirst = setOf(entryId)))
        }
    }

    private fun handOff() {
        scope.launch { if (connection.state().isLinked) work.handOff() }
    }

    private suspend fun sendLocalEdits() {
        var firstUnsent: TimeMark? = null
        localChanges.writes().collectLatest {
            val first = firstUnsent ?: TimeSource.Monotonic.markNow().also { firstUnsent = it }
            delay(minOf(QUIET, MAX_WAIT - first.elapsedNow()))
            firstUnsent = null
            runs.request(SyncRequest(onlyIfChanged = true))
        }
    }

    private suspend fun entryResumeContinues(): Long? = handler.await {
        sync_hand_offQueries.lastReadEntry(profileStore.currentProfileId).awaitAsOneOrNull()
    }

    private companion object {
        val QUIET: Duration = 5.seconds
        val MAX_WAIT: Duration = 60.seconds
    }
}
