package eu.kanade.presentation.more.settings.screen.textrecognition.models

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.model.artifacts.api.descriptor.ModelArtifactId
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.text.recognition.ui.models.TextRecognitionStoredModels
import mihon.text.recognition.ui.models.observeStoredModels
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Stored model revisions sorted by what the profile's saved recognition settings read with them. */
internal class ModelArtifactStorageScreenModel(
    private val store: ModelArtifactStore = Injekt.get(),
    hostActions: TextRecognitionHostActions = Injekt.get(),
) : ScreenModel {
    val stored: StateFlow<TextRecognitionStoredModels?> =
        hostActions.observeStoredModels(store).stateIn(screenModelScope, SharingStarted.Eagerly, null)

    fun delete(artifact: ModelArtifactId) {
        screenModelScope.launch { store.delete(artifact) }
    }

    /** Deletes every model no component of this build asks for. */
    fun deleteObsolete() {
        val obsolete = stored.value?.obsolete.orEmpty().map { it.descriptor.id }.distinct()
        screenModelScope.launch { obsolete.forEach { store.delete(it) } }
    }
}
