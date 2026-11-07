package app.katari.sync.ui.question

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.sync.client.account.AccountCall
import app.katari.sync.client.account.SyncAccount
import app.katari.sync.client.kind.question.ProfileDeletionAnswer
import app.katari.sync.client.question.Questions
import app.katari.sync.protocol.device.DeviceId
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import mihon.app.di.AppUiScope
import tachiyomi.core.common.util.lang.launchIO

/**
 * Needs your answer (D018, D029): every open question with what it names resolved, and the answers, each a change
 * like any edit that settles the question on every device once synced. Devices are named from the account's list,
 * read once as the screen opens; without the server they are named generically.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class SyncQuestionsViewModel(
    private val questions: Questions,
    private val names: QuestionNames,
    private val account: SyncAccount,
) : ViewModel() {
    private val devices = MutableStateFlow<Map<DeviceId, String>>(emptyMap())

    /** Null until first read. */
    val open: StateFlow<List<NamedQuestion>?> = combine(questions.open(), devices) { open, devices ->
        names.name(open, devices)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MILLIS), null)

    init {
        viewModelScope.launchIO {
            (account.devices() as? AccountCall.Done)?.let { listed ->
                devices.value = listed.value.associate { it.id to it.device.description.name }
            }
        }
    }

    fun keepMigration(question: NamedQuestion.Migration, target: String) {
        viewModelScope.launchIO { questions.keepMigration(question.question.from, target) }
    }

    fun keepNotes(question: NamedQuestion.Notes, notes: String) {
        viewModelScope.launchIO { questions.keepNotes(question.question.entryKey, notes) }
    }

    fun answerProfileDeletion(question: NamedQuestion.ProfileDeletion, answer: ProfileDeletionAnswer) {
        viewModelScope.launchIO { questions.answerProfileDeletion(question.question.profileUuid, answer) }
    }

    private companion object {
        const val STOP_AFTER_MILLIS = 5_000L
    }
}
