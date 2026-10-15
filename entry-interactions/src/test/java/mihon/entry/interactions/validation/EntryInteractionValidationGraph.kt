package mihon.entry.interactions.validation

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import io.mockk.mockk
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.text.recognition.api.TextRecognitionFeature
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.translation.api.TranslationFeature
import mihon.translation.api.host.TranslationHostActions
import mihon.tts.api.TtsFeature
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller

/** The Entry Features this module contributes, with the host services the validation environment controls. */
@DependencyGraph(
    AppScope::class,
    bindingContainers = [
        ControlledEntryHostBindings::class,
        ControlledEntryPointBindings::class,
    ],
)
interface EntryInteractionValidationGraph : ProductionValidationGraph, ViewModelGraph {
    // The translation, text recognition, speech and model artifact runtimes are app modules, so only the app graph
    // has the real ones.
    @Provides
    fun translationHostActions(): TranslationHostActions = mockk(relaxed = true)

    @Provides
    fun translationFeature(): TranslationFeature = mockk(relaxed = true)

    @Provides
    fun textRecognitionFeature(): TextRecognitionFeature = mockk(relaxed = true)

    @Provides
    fun ttsFeature(): TtsFeature = mockk(relaxed = true)

    @Provides
    fun textRecognitionHostActions(): TextRecognitionHostActions = mockk(relaxed = true)

    @Provides
    fun modelArtifactStore(): ModelArtifactStore = mockk(relaxed = true)

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Includes appServices: ControlledAppServiceBindings,
            @Provides profilePreferenceOwners: ProfilePreferenceOwnerInstaller,
        ): EntryInteractionValidationGraph
    }
}
