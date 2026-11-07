package app.katari.sync.ui.question

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.sync.client.question.Questions
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import mihon.app.di.AppUiScope

/** The question an open entry's screen shows as a banner, if one waits about it. */
@AssistedInject
class EntryQuestionViewModel(
    @Assisted entryId: Long,
    questions: Questions,
    names: QuestionNames,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(entryId: Long): EntryQuestionViewModel
    }

    val question: StateFlow<EntryQuestion?> = questions.open()
        .map { open -> EntryQuestion.of(open, entryId, names::entryId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
