package app.katari.sync.ui.status

import app.katari.sync.client.connection.SyncStop
import app.katari.sync.client.run.SyncStep
import app.katari.sync.client.status.SyncStatus

/**
 * The one thing the Sync rows say about sync (D028, D054), chosen from everything [SyncStatus] holds at once. What
 * needs the person comes before what sync is doing, lasting trouble before passing states, and doing before done; a
 * note on files rides along with being up to date.
 */
sealed interface SyncHeadline {
    data object NotSetUp : SyncHeadline

    data class Removed(val server: String?) : SyncHeadline

    data class AccountDisabled(val server: String?) : SyncHeadline

    data object SignInAgain : SyncHeadline

    data object AppTooOld : SyncHeadline

    data class ServerTooOld(val server: String) : SyncHeadline

    data object ChoicesWaiting : SyncHeadline

    data class Questions(val count: Int) : SyncHeadline

    data class UnreachableLong(val server: String, val sinceMillis: Long) : SyncHeadline

    /** Records of kinds a newer app version syncs wait here until this app is updated (D031). */
    data class HeldKinds(val kinds: List<String>) : SyncHeadline

    /** A first download or full resync; [percent] done, or null when unknown. */
    data class Downloading(val percent: Int?) : SyncHeadline

    /**
     * A full resync filling in a server that lost what it had, such as one restored from an older copy (D021);
     * [percent] of the account taken so far, or null when unknown.
     */
    data class CatchingUp(val percent: Int?) : SyncHeadline

    /** A step of a long run, named with how far it got. */
    data class Step(val step: SyncStep) : SyncHeadline

    data object FirstDownloadWaitsForWifi : SyncHeadline

    data object Syncing : SyncHeadline

    /** [count] changes made here wait, because there is no connection or [server] doesn't answer. */
    data class Unsent(val count: Long, val unreachableServer: String?) : SyncHeadline

    data object Offline : SyncHeadline

    data class Unreachable(val server: String) : SyncHeadline

    /** The last run failed on this device; [lastSyncedAtMillis] is null before the first sync finished. */
    data class Failed(val lastSyncedAtMillis: Long?) : SyncHeadline

    /** [atMillis] is null before the first sync finished; [files] notes covers and fonts that haven't moved. */
    data class UpToDate(val atMillis: Long?, val files: FilesNote?) : SyncHeadline

    sealed interface FilesNote {
        data class WaitingForWifi(val count: Long) : FilesNote

        data class MissingOnServer(val count: Long) : FilesNote
    }

    /** Whether this is trouble the person should notice: shown in colour and as a warning in the Library toolbar. */
    val needsAttention: Boolean
        get() = when (this) {
            is Removed, is AccountDisabled, SignInAgain, AppTooOld, is ServerTooOld, ChoicesWaiting, is Questions,
            is UnreachableLong, is Failed,
            -> true
            else -> false
        }
}

fun SyncStatus.headline(nowMillis: Long): SyncHeadline {
    val link = link
    when (link) {
        SyncStatus.Link.NotSetUp -> return SyncHeadline.NotSetUp
        is SyncStatus.Link.Ended -> return when (link.reason) {
            SyncStop.ACCOUNT_DISABLED -> SyncHeadline.AccountDisabled(link.server?.host)
            else -> SyncHeadline.Removed(link.server?.host)
        }
        is SyncStatus.Link.Linked -> Unit
    }
    val server = link.server.host
    when (link.stopped) {
        SyncStop.SIGN_IN_AGAIN -> return SyncHeadline.SignInAgain
        SyncStop.APP_TOO_OLD -> return SyncHeadline.AppTooOld
        SyncStop.SERVER_TOO_OLD -> return SyncHeadline.ServerTooOld(server)
        SyncStop.REMOVED -> return SyncHeadline.Removed(server)
        SyncStop.ACCOUNT_DISABLED -> return SyncHeadline.AccountDisabled(server)
        null -> Unit
    }
    val activity = activity
    if (activity == SyncStatus.Activity.WaitingForChoices) return SyncHeadline.ChoicesWaiting
    if (questions.isNotEmpty()) return SyncHeadline.Questions(questions.size)
    if (activity is SyncStatus.Activity.Unreachable && activity.isLong(nowMillis)) {
        return SyncHeadline.UnreachableLong(server, activity.sinceMillis)
    }
    if (heldKinds.isNotEmpty()) return SyncHeadline.HeldKinds(heldKinds)
    return when (activity) {
        is SyncStatus.Activity.Syncing -> when {
            activity.catchingUp -> SyncHeadline.CatchingUp(activity.percent)
            activity.percent != null -> SyncHeadline.Downloading(activity.percent)
            else -> activity.step?.let(SyncHeadline::Step) ?: SyncHeadline.Syncing
        }
        SyncStatus.Activity.WaitingForWifi -> SyncHeadline.FirstDownloadWaitsForWifi
        SyncStatus.Activity.Offline -> if (unsent > 0) SyncHeadline.Unsent(unsent, null) else SyncHeadline.Offline
        is SyncStatus.Activity.Unreachable -> if (unsent > 0) {
            SyncHeadline.Unsent(unsent, server)
        } else {
            SyncHeadline.Unreachable(server)
        }
        is SyncStatus.Activity.Failed -> SyncHeadline.Failed(activity.lastSyncedAtMillis)
        is SyncStatus.Activity.UpToDate -> SyncHeadline.UpToDate(activity.atMillis, filesNote())
        // Already handled above, or not linked
        SyncStatus.Activity.WaitingForChoices, SyncStatus.Activity.Idle -> SyncHeadline.UpToDate(null, filesNote())
    }
}

private fun SyncStatus.filesNote(): SyncHeadline.FilesNote? = when {
    files.waitingForWifi -> SyncHeadline.FilesNote.WaitingForWifi(files.toUpload + files.toDownload)
    files.missingOnServer > 0 -> SyncHeadline.FilesNote.MissingOnServer(files.missingOnServer)
    else -> null
}
