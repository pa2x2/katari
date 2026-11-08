package app.katari.sync.ui.question

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.sync.client.account.AccountCall
import app.katari.sync.client.account.SyncAccount
import app.katari.sync.client.kind.question.ProfileDeletionAnswer
import app.katari.sync.client.question.Question
import app.katari.sync.client.question.Questions
import app.katari.sync.protocol.device.DeviceId
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import mihon.app.di.AppUiScope
import mihon.core.metro.AppCoroutineScope
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.data.ActiveProfileProvider

/**
 * Needs your answer (D018, D029): every open question with what it names resolved, and the answers, each a change
 * like any edit that settles the question on every device once synced. Devices are named from the account's list,
 * read once as the screen opens; without the server they are named generically.
 *
 * Deleting a profile waits out an undo window first (D025): the question is hidden meanwhile, and nothing is written,
 * so nothing reaches the server, until the window passes.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class SyncQuestionsViewModel(
    private val questions: Questions,
    private val names: QuestionNames,
    private val account: SyncAccount,
    activeProfile: ActiveProfileProvider,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : ViewModel() {
    private val devices = MutableStateFlow<Map<DeviceId, String>>(emptyMap())

    /** The profiles, by uuid, whose deletion waits out its undo window. */
    private val deleting = MutableStateFlow<Set<String>>(emptySet())

    private val finished = Channel<Unit>(Channel.CONFLATED)

    /** Null until first read. */
    val open: StateFlow<List<NamedQuestion>?> = combine(
        questions.open(),
        devices,
        activeProfile.activeProfileIdFlow,
        deleting,
    ) { open, devices, activeProfileId, deleting ->
        val shown = open.filterNot { it is Question.ProfileDeletion && it.profileUuid in deleting }
        names.name(shown, devices, account.thisDevice(), activeProfileId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MILLIS), null)

    /** Hears once an answer settled the last open question (D025), so the screen goes back to where it came from. */
    val allAnswered: Flow<Unit> = finished.receiveAsFlow()

    init {
        viewModelScope.launchIO {
            (account.devices() as? AccountCall.Done)?.let { listed ->
                devices.value = listed.value.associate { it.id to it.device.description.name }
            }
        }
    }

    fun keepMigration(question: NamedQuestion.Migration, target: String) {
        viewModelScope.launchIO {
            questions.keepMigration(question.question.from, target)
            answered()
        }
    }

    fun keepNotes(question: NamedQuestion.Notes, notes: String) {
        viewModelScope.launchIO {
            questions.keepNotes(question.question.entryKey, notes)
            answered()
        }
    }

    fun keepProfile(question: NamedQuestion.ProfileDeletion) {
        viewModelScope.launchIO {
            questions.answerProfileDeletion(question.question.profileUuid, ProfileDeletionAnswer.KEEP)
            answered()
        }
    }

    /** Hides [question] while its deletion can be undone; [undoDeletion] or [deleteProfile] then settles it. */
    fun startDeletion(question: NamedQuestion.ProfileDeletion) {
        deleting.update { it + question.question.profileUuid }
    }

    fun undoDeletion(question: NamedQuestion.ProfileDeletion) {
        deleting.update { it - question.question.profileUuid }
    }

    /**
     * Deletes the profile once its undo window passed, also when the screen closes first: leaving without undoing
     * accepts it, so the answer is written beyond this screen's life.
     */
    fun deleteProfile(question: NamedQuestion.ProfileDeletion) {
        val uuid = question.question.profileUuid
        appScope.launchIO {
            questions.answerProfileDeletion(uuid, ProfileDeletionAnswer.DELETE)
            // Hidden until answered, so the card doesn't show again meanwhile
            deleting.update { it - uuid }
            answered()
        }
    }

    private suspend fun answered() {
        if (questions.open().first().isEmpty()) finished.send(Unit)
    }

    private companion object {
        const val STOP_AFTER_MILLIS = 5_000L
    }
}
