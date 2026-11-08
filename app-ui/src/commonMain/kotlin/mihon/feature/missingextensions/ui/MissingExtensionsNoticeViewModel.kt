package mihon.feature.missingextensions.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import mihon.app.di.AppUiScope
import mihon.feature.missingextensions.model.MissingExtension
import mihon.feature.missingextensions.suggestion.MissingExtensionSuggestion
import kotlin.time.Duration.Companion.seconds

/** What the notices and rows that open the missing-extensions sheet show. */
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class MissingExtensionsNoticeViewModel(private val suggestion: MissingExtensionSuggestion) : ViewModel() {
    val installable: StateFlow<List<MissingExtension>> = suggestion.installable
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), emptyList())

    /** Whether a first sync is downloading the library, so the counts still climb; null until known. */
    val libraryArriving: StateFlow<Boolean?> = suggestion.libraryArriving
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), null)

    val libraryCard: StateFlow<List<MissingExtension>> = suggestion.libraryCard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), emptyList())

    fun answerLibraryCard() {
        suggestion.answered(libraryCard.value)
    }
}
