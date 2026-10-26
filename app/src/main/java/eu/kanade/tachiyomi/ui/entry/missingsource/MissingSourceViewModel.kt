package eu.kanade.tachiyomi.ui.entry.missingsource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.store.isInstallable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import mihon.app.di.AppUiScope
import tachiyomi.core.common.util.lang.launchIO
import kotlin.time.Duration.Companion.seconds

/** Finds a store listing that provides a missing source and installs it, from a sheet it opens and closes. */
@AssistedInject
class MissingSourceViewModel(
    @Assisted private val sourceId: Long,
    private val extensionManager: ExtensionManager,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(sourceId: Long): MissingSourceViewModel
    }

    private val _sheetShown = MutableStateFlow(false)
    val sheetShown: StateFlow<Boolean> = _sheetShown.asStateFlow()

    /** `null` when no added store lists it, or none that this app can install from. */
    val listing: StateFlow<Extension.Available?> = extensionManager.availableExtensionsFlow
        .map { extensionManager.getListingOfSource(sourceId)?.takeIf { it.isInstallable && !it.isTorrent } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), null)

    private val _installStep = MutableStateFlow<InstallStep>(InstallStep.Idle)
    val installStep: StateFlow<InstallStep> = _installStep.asStateFlow()

    fun showSheet() {
        _sheetShown.value = true
    }

    fun dismissSheet() {
        _sheetShown.value = false
    }

    fun install(listing: Extension.Available) {
        viewModelScope.launchIO {
            extensionManager.installExtension(listing)
                .takeWhile {
                    _installStep.value = it
                    !it.isCompleted()
                }
                .collect {}
            // Once installed the source is no longer missing, which leaves nothing for the sheet to offer
            if (_installStep.value == InstallStep.Installed) dismissSheet()
        }
    }
}
