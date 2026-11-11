package app.katari.sync.client.status

import app.katari.sync.client.connection.SyncStop
import app.katari.sync.client.question.Question
import app.katari.sync.client.run.SyncStep
import app.katari.sync.client.run.SyncSteps
import app.katari.sync.client.trigger.LiveUpdatesState
import okhttp3.HttpUrl
import kotlin.time.Duration.Companion.days

/**
 * Everything the Sync page, the More row and sync's status icon show about sync (D028, D050, D054, D055), in one
 * value: whether the device is linked and to what, what it is doing, whether changes come as they happen, what waits to
 * be sent and why, and what waits for the person.
 */
data class SyncStatus(
    val link: Link,
    /** What the device is doing; [Activity.Idle] while it isn't linked. */
    val activity: Activity,
    /** Changes made here that the server doesn't have yet. */
    val unsent: Long,
    /** Why [unsent] changes aren't on their way now; null while they are, or while there are none. */
    val unsentBecause: UnsentReason?,
    val questions: List<Question>,
    /** Kinds of records a newer app version synced, held until this one is updated (D031). */
    val heldKinds: List<String>,
    /** Records that failed to apply here, held aside until the app is updated while the rest sync (D067). */
    val heldAside: Long,
    /** Custom covers and fonts still to move; 0 everywhere while the device isn't linked. */
    val files: Files,
    /**
     * Whether other devices' changes come here as they happen. [LiveUpdatesState.NOT_WORKING] only while the server
     * answers runs: one that can't be reached is the problem the status tells instead.
     */
    val live: LiveUpdatesState,
) {
    sealed interface Link {
        /** Standalone, never set up or after disconnecting. */
        data object NotSetUp : Link

        /**
         * Standalone again because of [reason] ([SyncStop.REMOVED] or [SyncStop.ACCOUNT_DISABLED]), which stays until
         * dismissed or the device signs in again (D030).
         */
        data class Ended(val reason: SyncStop, val server: HttpUrl?, val accountName: String?) : Link

        /** Signed in to [accountName] on [server]; [stopped] when syncing is paused until something changes. */
        data class Linked(val server: HttpUrl, val accountName: String?, val stopped: SyncStop?) : Link
    }

    sealed interface Activity {
        data object Idle : Activity

        /**
         * A run is going; during a first download or full resync, [percent] says how far it got, and is null
         * otherwise. [step] is the step of a run long enough to name, with its own progress ([SyncSteps]). Both change
         * only as what they show does, and at most every couple of seconds. [catchingUp] while the device resyncs a
         * server that lost what it had, such as one restored from an older copy, to fill it in again
         * ([app.katari.sync.client.connection.SyncStage.RESYNC]).
         */
        data class Syncing(val percent: Int?, val step: SyncStep? = null, val catchingUp: Boolean = false) : Activity

        /** Waits for the person's first-sync choices ([app.katari.sync.client.account.SyncAccount.firstSyncProposal]). */
        data object WaitingForChoices : Activity

        /** A first download waits for Wi-Fi (D022). */
        data object WaitingForWifi : Activity

        /** Changes wait for Wi-Fi, the person having chosen not to sync them on mobile data. */
        data object ChangesWaitForWifi : Activity

        /** The person paused syncing on this device. */
        data object Paused : Activity

        data object Offline : Activity

        /** The server hasn't answered since [sinceMillis]. */
        data class Unreachable(val sinceMillis: Long) : Activity {
            /** Past a day, which the More row says (D028). */
            fun isLong(nowMillis: Long): Boolean = nowMillis - sinceMillis > 1.days.inWholeMilliseconds
        }

        /**
         * The last run that had something to do failed on this device, not for want of an answer, such as on a
         * change that couldn't be applied; it last synced at [lastSyncedAtMillis], null before the first sync.
         */
        data class Failed(val lastSyncedAtMillis: Long?) : Activity

        /** Last synced at [atMillis]; null when the first sync hasn't finished yet. */
        data class UpToDate(val atMillis: Long?) : Activity
    }

    /** Custom covers and fonts that haven't moved yet (D016, D022, D028). */
    data class Files(
        /** Files this device holds that the server may lack; the records naming them wait with them. */
        val toUpload: Long,
        /** Files the records here name that aren't on this device yet. */
        val toDownload: Long,
        /**
         * Files the records here name that the server said it doesn't have, or sent damaged: their records stay, and
         * each is asked for again a day later.
         */
        val missingOnServer: Long,
        /** Whether [toUpload] and [toDownload] wait for Wi-Fi, mobile data not being allowed for files (D022). */
        val waitingForWifi: Boolean,
    ) {
        companion object {
            val NONE = Files(toUpload = 0, toDownload = 0, missingOnServer = 0, waitingForWifi = false)
        }
    }

    enum class UnsentReason {
        NO_CONNECTION,

        /** A network, but the server doesn't answer. */
        SERVER_UNREACHABLE,

        /** Syncing is stopped, such as until the app or the server is updated, or until the person signs in again. */
        STOPPED,

        /** The person paused syncing. */
        PAUSED,

        /** Changes wait for Wi-Fi, as the person chose. */
        WAITING_FOR_WIFI,
    }

    /** Whether something waits for the person, which More marks with a dot (D028). */
    val needsAttention: Boolean
        get() = questions.isNotEmpty() || activity == Activity.WaitingForChoices || link is Link.Ended ||
            (link as? Link.Linked)?.stopped != null

    /** Whether the device is linked and syncing isn't stopped. */
    val isOn: Boolean
        get() = link is Link.Linked && link.stopped == null

    /**
     * Whether a sync asked for now would run: syncing [isOn], no run is going, and the first-sync choices aren't
     * waiting, which hold every run as a stop does.
     */
    val canSyncNow: Boolean
        get() = isOn && activity !is Activity.Syncing && activity != Activity.WaitingForChoices
}
