package app.katari.missingextensions.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.missingextensions.otherdevices.OtherDeviceChanges
import app.katari.missingextensions.otherdevices.OtherDeviceSuggestions
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.seconds

/** What the notices that offer the installs and uninstalls of the account's other devices show. */
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class OtherDeviceExtensionsNoticeViewModel(private val suggestions: OtherDeviceSuggestions) : ViewModel() {
    val offered: StateFlow<OtherDeviceChanges> = suggestions.changes
        .map { it ?: OtherDeviceChanges.NONE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), OtherDeviceChanges.NONE)

    val libraryCard: StateFlow<OtherDeviceChanges> = suggestions.libraryCard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), OtherDeviceChanges.NONE)

    fun answerLibraryCard() {
        suggestions.answered(libraryCard.value)
    }
}
