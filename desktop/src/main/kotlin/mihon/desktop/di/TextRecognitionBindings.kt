package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.textrecognition.language.LibrarySeriesLanguages
import eu.kanade.presentation.more.settings.screen.textrecognition.playground.TextRecognitionPlaygroundImageDecoder
import mihon.desktop.models.NoModelArtifactStore
import mihon.desktop.textrecognition.NoLibrarySeriesLanguages
import mihon.desktop.textrecognition.NoTextRecognitionFeature
import mihon.desktop.textrecognition.NoTextRecognitionHostActions
import mihon.desktop.textrecognition.NoTextRecognitionPlaygroundImageDecoder
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.text.recognition.api.TextRecognitionFeature
import mihon.text.recognition.api.host.TextRecognitionHostActions

@BindingContainer
interface TextRecognitionBindings {
    @Binds
    val NoTextRecognitionFeature.textRecognitionFeature: TextRecognitionFeature

    @Binds
    val NoTextRecognitionHostActions.textRecognitionHostActions: TextRecognitionHostActions

    @Binds
    val NoModelArtifactStore.modelArtifactStore: ModelArtifactStore

    @Binds
    val NoTextRecognitionPlaygroundImageDecoder.playgroundImageDecoder: TextRecognitionPlaygroundImageDecoder

    @Binds
    val NoLibrarySeriesLanguages.librarySeriesLanguages: LibrarySeriesLanguages
}
