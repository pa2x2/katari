package app.katari.text.recognition.spi.model

import app.katari.model.artifacts.api.descriptor.ModelArtifactDescriptor
import app.katari.model.artifacts.api.state.InstalledModelArtifact

/**
 * Verified installations of the models a component declared. The runtime resolves them before every run, so
 * components never observe a missing or partially downloaded model.
 */
class TextRecognitionModels(
    installed: List<InstalledModelArtifact>,
) {
    private val byDescriptor = installed.associateBy(InstalledModelArtifact::descriptor)

    operator fun get(descriptor: ModelArtifactDescriptor): InstalledModelArtifact =
        requireNotNull(byDescriptor[descriptor]) {
            "Model ${descriptor.id.value}@${descriptor.revision} was not resolved for this run"
        }
}
