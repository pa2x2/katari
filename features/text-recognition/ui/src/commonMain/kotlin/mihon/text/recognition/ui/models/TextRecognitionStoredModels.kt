package mihon.text.recognition.ui.models

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.model.artifacts.api.state.StoredModelArtifact
import mihon.text.recognition.api.host.TextRecognitionHostActions

/** Stored models sorted by what they are for, so storage screens can explain and clean them up. */
data class TextRecognitionStoredModels(
    /** Models the configuration reads languages with. */
    val inUse: List<InUse>,
    /** Models the catalog can use but the configuration currently does not. */
    val unused: List<StoredModelArtifact>,
    /** Models no component of this build asks for, for example those of a removed engine. */
    val obsolete: List<StoredModelArtifact>,
) {
    val totalBytes: Long
        get() = inUse.sumOf { it.artifact.storedBytes } + (unused + obsolete).sumOf { it.storedBytes }

    val obsoleteBytes: Long
        get() = obsolete.sumOf { it.storedBytes }

    /** A stored model and the languages read with it; [everyLanguage] when every readable language needs it. */
    data class InUse(
        val artifact: StoredModelArtifact,
        val languages: List<LanguageTag>,
        val everyLanguage: Boolean,
    )
}

/** Sorts [stored] models by how this usage needs them, keeping the store's order within each group. */
fun TextRecognitionModelUsage.sort(stored: List<StoredModelArtifact>): TextRecognitionStoredModels {
    val (known, obsolete) = stored.partition { it.descriptor.id in this.known }
    val (inUse, unused) = known.partition { it.descriptor.id in languages }
    return TextRecognitionStoredModels(
        inUse = inUse.map { artifact ->
            val used = languages.getValue(artifact.descriptor.id)
            TextRecognitionStoredModels.InUse(artifact, used, everyLanguage = used.size == readableLanguages)
        },
        unused = unused,
        obsolete = obsolete,
    )
}

/** The models in [store], sorted by how the stored recognition configuration uses them. */
fun TextRecognitionHostActions.observeStoredModels(store: ModelArtifactStore): Flow<TextRecognitionStoredModels> =
    combine(store.observeStored(), observeConfiguration()) { stored, configuration ->
        modelUsage(configuration).sort(stored)
    }
