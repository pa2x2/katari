package eu.kanade.presentation.more.settings.screen.textrecognition.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.model.artifacts.api.ModelArtifactStore
import app.katari.model.artifacts.api.descriptor.ModelArtifactId
import app.katari.text.recognition.api.host.TextRecognitionHostActions
import app.katari.text.recognition.ui.models.TextRecognitionStoredModels
import app.katari.text.recognition.ui.models.observeStoredModels
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Stored model revisions sorted by what the profile's saved recognition settings read with them. */
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class ModelArtifactStorageViewModel(
    private val store: ModelArtifactStore,
    hostActions: TextRecognitionHostActions,
) : ViewModel() {
    val stored: StateFlow<TextRecognitionStoredModels?> =
        hostActions.observeStoredModels(store).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun delete(artifact: ModelArtifactId) {
        viewModelScope.launch { store.delete(artifact) }
    }

    /** Deletes every model no component of this build asks for. */
    fun deleteObsolete() {
        val obsolete = stored.value?.obsolete.orEmpty().map { it.descriptor.id }.distinct()
        viewModelScope.launch { obsolete.forEach { store.delete(it) } }
    }
}
