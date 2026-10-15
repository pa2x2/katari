package mihon.app.di.injekt

import mihon.app.di.ApplicationFeatureGraph
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.text.recognition.api.TextRecognitionFeature
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.translation.api.TranslationFeature
import mihon.translation.api.host.TranslationHostActions
import mihon.tts.api.TtsFeature
import mihon.tts.api.host.TtsHostActions
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory

/** Serves the graph's application Features to code that still resolves them through Injekt. */
fun InjektRegistrar.addApplicationFeatures(graph: ApplicationFeatureGraph) {
    // Profile copy and backup derive preference ownership from every registered owner, so owners register at
    // startup instead of when their Feature is first used.
    graph.profilePreferenceOwners

    addSingletonFactory<ModelArtifactStore> { graph.modelArtifactStore }
    addSingletonFactory<TextRecognitionFeature> { graph.textRecognitionFeature }
    addSingletonFactory<TextRecognitionHostActions> { graph.textRecognitionHostActions }
    addSingletonFactory<TranslationFeature> { graph.translationFeature }
    addSingletonFactory<TranslationHostActions> { graph.translationHostActions }
    addSingletonFactory<TtsFeature> { graph.ttsFeature }
    addSingletonFactory<TtsHostActions> { graph.ttsHostActions }
}
