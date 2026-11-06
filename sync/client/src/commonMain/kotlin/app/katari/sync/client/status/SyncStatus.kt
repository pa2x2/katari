package app.katari.sync.client.status

import app.katari.sync.client.connection.SyncStop
import app.katari.sync.client.question.Question
import okhttp3.HttpUrl
import kotlin.time.Duration.Companion.days

/**
 * Everything the Sync page, the More row and the Library toolbar show about sync (D028, D050, D054, D055), in one
 * value: whether the device is linked and to what, what it is doing, what waits to be sent and why, and what waits
 * for the person.
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
         * A run is going; during a first download or full resync, [progress] says how far it got, from 0 to 1, and is
         * null otherwise.
         */
        data class Syncing(val progress: Float?) : Activity

        /** Waits for the person's first-sync choices ([app.katari.sync.client.account.SyncAccount.firstSyncProposal]). */
        data object WaitingForChoices : Activity

        /** A first download waits for Wi-Fi (D022). */
        data object WaitingForWifi : Activity

        data object Offline : Activity

        /** The server hasn't answered since [sinceMillis]. */
        data class Unreachable(val sinceMillis: Long) : Activity {
            /** Past a day, which the More row says (D028). */
            fun isLong(nowMillis: Long): Boolean = nowMillis - sinceMillis > 1.days.inWholeMilliseconds
        }

        /** Last synced at [atMillis]; null when the first sync hasn't finished yet. */
        data class UpToDate(val atMillis: Long?) : Activity
    }

    enum class UnsentReason {
        NO_CONNECTION,

        /** A network, but the server doesn't answer. */
        SERVER_UNREACHABLE,

        /** Syncing is stopped, such as until the app or the server is updated, or until the person signs in again. */
        STOPPED,
    }

    /** Whether something waits for the person, which More marks with a dot (D028). */
    val needsAttention: Boolean
        get() = questions.isNotEmpty() || activity == Activity.WaitingForChoices || link is Link.Ended ||
            (link as? Link.Linked)?.stopped != null
}
