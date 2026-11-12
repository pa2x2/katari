package app.katari.sync.client.trigger

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.profiles.core.ProfileStore
import app.katari.sync.client.account.DeviceListFollowing
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.handoff.EntryHandOff
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.record.ChangeTransferRule
import app.katari.sync.client.run.SyncRequest
import app.katari.sync.client.run.SyncRuns
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import tachiyomi.data.DatabaseHandler
import kotlin.time.Duration.Companion.minutes

/**
 * When sync runs (D021), on every platform; the platforms call in when the app opens or closes and when it comes to the
 * front or leaves it, and the readers and the player when a period of reading or watching was recorded. A desktop app
 * stays open while its window is minimized or behind others; on Android the two go together.
 *
 * - A local edit: a run at once, and while edits keep coming, as a library update's do, at most every couple of
 *   seconds ([RunPacing]). Marking 200 chapters read is one write, so one push. Reading position counts as an edit.
 * - Leaving or pausing a reader or the player, and the app leaving the front (screen off, another app): the
 *   platform's hand-off work ([SyncBackgroundWork.handOff]), which finishes in the background and retries until the
 *   server has it.
 * - The app coming to the front: a run that fetches the entry Resume continues first (D052), then pulls and pushes.
 * - While open: the live channel ([LiveUpdates]), which also keeps the account's device list current
 *   ([DeviceListFollowing]).
 * - In the background: the platform's periodic work while background sync is on, at the interval the person chose.
 *   With instant updates on as well, the platform keeps the app running and the live channel open
 *   ([SyncBackgroundWork.keepConnected]); the periodic work goes on under it, for when the channel can't.
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
    private val changeRule: ChangeTransferRule,
    private val deviceList: DeviceListFollowing,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isOpen = MutableStateFlow(false)
    private val isConnectedInBackground = MutableStateFlow(false)
    private var started = false

    /** Starts the triggers that watch by themselves, once a process, after capture has resumed. */
    fun start() {
        synchronized(this) {
            if (started) return
            started = true
        }
        val linked = connection.states().map { it.isLinked }.distinctUntilChanged()
        val listening = combine(isOpen, isConnectedInBackground) { open, connected -> open || connected }
        scope.launch { live.wanted(listening).collectLatest { wanted -> if (wanted) live.listen() } }
        scope.launch { linked.collectLatest { isLinked -> if (isLinked) sendLocalEdits() } }
        scope.launch { linked.collectLatest { isLinked -> if (isLinked) deviceList.follow() } }
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
        scope.launch {
            combine(
                linked,
                preferences.syncInBackground.changes(),
                preferences.instant.changes(),
                preferences.paused.changes(),
                isOpen,
            ) { isLinked, inBackground, instant, paused, open ->
                (isLinked && inBackground && instant && !paused) to open
            }
                .distinctUntilChanged()
                // Told again whenever the app opens: a platform may only let the app start it from the front
                .collect { (connected, _) -> work.keepConnected(connected) }
        }
        // What waited for syncing to resume, or for Wi-Fi, goes as soon as it may
        scope.launch {
            combine(linked, preferences.paused.changes(), changeRule.allows()) { isLinked, paused, allowed ->
                isLinked && !paused && allowed
            }
                .distinctUntilChanged()
                .drop(1)
                .collect { mayGo -> if (mayGo) runs.request(SyncRequest()) }
        }
    }

    /** Ends what [start] started and waits for it, as a test device does before its database closes. */
    internal suspend fun stop() {
        scope.coroutineContext.job.cancelAndJoin()
    }

    fun appOpened() {
        isOpen.value = true
    }

    fun appClosed() {
        isOpen.value = false
    }

    /** The platform keeps the app running for the live channel ([SyncBackgroundWork.keepConnected]), or stopped. */
    fun connectedInBackground(connected: Boolean) {
        isConnectedInBackground.value = connected
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

    private suspend fun sendLocalEdits(): Nothing = coroutineScope {
        val pacing = RunPacing()
        launch { localChanges.writes().collect { pacing.hear() } }
        pacing.follow { runs.request(SyncRequest(onlyIfChanged = true)) }
    }

    private suspend fun entryResumeContinues(): Long? = handler.await {
        sync_hand_offQueries.lastReadEntry(profileStore.currentProfileId).awaitAsOneOrNull()
    }
}
