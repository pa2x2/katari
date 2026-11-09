package app.katari.sync.ui.notice

import app.katari.core.metro.AppCoroutineScope
import app.katari.sync.client.connection.SyncStop
import app.katari.sync.client.question.Question
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.ui.status.SyncHeadline
import app.katari.sync.ui.status.SyncStatusState
import app.katari.sync.ui.status.headline
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile
import kotlin.time.Clock

/**
 * Posts [SyncNotifications] while the app is closed or in the background (D028): when a question arrives, and when
 * this device is signed out, removed from the account or the account is disabled. While the app is in front, its
 * screens say so instead: sync's status icon and the dot on More, and for a stop, which keeps every later change on
 * this device until the person acts, one message on the screen they're on ([stoppedInFront]). What was there when
 * the process started was told by an earlier one, so only changes after it notify; a notification goes once what it
 * told is over, wherever the app is.
 */
@Inject
@SingleIn(AppScope::class)
class SyncNotices(
    private val statusState: SyncStatusState,
    private val notifications: SyncNotifications,
    @AppCoroutineScope private val scope: CoroutineScope,
) {
    @Volatile
    private var inFront = false

    private val stopsInFront = MutableSharedFlow<SyncHeadline>(extraBufferCapacity = 1)

    /** The headline of each stop that happens while the app is in front, for the screen it is on to tell. */
    val stoppedInFront: SharedFlow<SyncHeadline> = stopsInFront.asSharedFlow()

    fun cameToFront() {
        inFront = true
    }

    fun wentToBack() {
        inFront = false
    }

    fun start() {
        scope.launch {
            var previous: SyncStatus? = null
            statusState.status.filterNotNull().collect { status ->
                previous?.let { notice(it, status) }
                previous = status
            }
        }
    }

    private fun notice(previous: SyncStatus, status: SyncStatus) {
        val asked = previous.questions.mapTo(HashSet()) { it.subject() }
        val arrived = status.questions.filter { it.subject() !in asked }
        when {
            status.questions.isEmpty() && previous.questions.isNotEmpty() -> notifications.withdrawQuestions()
            arrived.isNotEmpty() && !inFront -> notifications.showQuestions(status.questions.size)
        }
        val stop = status.link.toldStop()
        when {
            stop == null && previous.link.toldStop() != null -> notifications.withdrawStopped()
            stop != null && stop != previous.link.toldStop() -> if (inFront) {
                stopsInFront.tryEmit(status.headline(Clock.System.now().toEpochMilliseconds()))
            } else {
                notifications.showStopped(stop, status.link.server())
            }
        }
    }

    // A question stays the same one while devices add to it, such as another migration target
    private fun Question.subject(): Any = when (this) {
        is Question.Migration -> this::class to from
        is Question.Notes -> this::class to entryKey
        is Question.ProfileDeletion -> this::class to profileUuid
    }

    // The stops the person has to act on; an app or server too old for the other resolves with an update
    private fun SyncStatus.Link.toldStop(): SyncStop? = when (this) {
        is SyncStatus.Link.Ended -> reason
        is SyncStatus.Link.Linked -> stopped?.takeIf { it == SyncStop.SIGN_IN_AGAIN || it == SyncStop.SERVER_RESET }
        SyncStatus.Link.NotSetUp -> null
    }

    private fun SyncStatus.Link.server() = when (this) {
        is SyncStatus.Link.Ended -> server
        is SyncStatus.Link.Linked -> server
        SyncStatus.Link.NotSetUp -> null
    }
}
