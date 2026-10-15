package mihon.app.di

import mihon.feature.runtime.FeatureRuntimeContributions
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.text.recognition.api.TextRecognitionFeature
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.translation.api.TranslationFeature
import mihon.translation.api.host.TranslationHostActions
import mihon.tts.api.TtsFeature
import mihon.tts.api.host.TtsHostActions
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner

/**
 * The application Features as code outside the graph reads them. The production graph and the production
 * validation graph both expose this, so validation sees the same Features the app installs.
 */
interface ApplicationFeatureGraph {
    val featureRuntimeContributions: FeatureRuntimeContributions

    /** Reading this registers every Feature's profile preference owner. */
    val profilePreferenceOwners: Set<InstalledProfilePreferenceOwner<*>>

    val modelArtifactStore: ModelArtifactStore
    val textRecognitionFeature: TextRecognitionFeature
    val textRecognitionHostActions: TextRecognitionHostActions
    val translationFeature: TranslationFeature
    val translationHostActions: TranslationHostActions
    val ttsFeature: TtsFeature
    val ttsHostActions: TtsHostActions
}
