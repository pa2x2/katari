package app.katari.sync.ui.arrival

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.core.metro.AppCoroutineScope
import app.katari.sync.client.arrival.ArrivedSetting
import app.katari.sync.client.arrival.SettingArrivals
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import tachiyomi.core.common.util.lang.launchIO

/**
 * The settings other devices changed here, for the Sync page's row and the list it opens. Keep mine changes this
 * device's sync choices, so it runs in the app's scope, as choices made on What syncs do.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class ArrivedSettingsViewModel(
    private val arrivals: SettingArrivals,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : ViewModel() {
    val changes: StateFlow<List<ArrivedSetting>> =
        arrivals.changes().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun keepMine(setting: ArrivedSetting) {
        appScope.launchIO { arrivals.keepMine(setting) }
    }
}
