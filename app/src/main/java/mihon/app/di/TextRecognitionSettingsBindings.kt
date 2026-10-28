package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.textrecognition.language.CatalogueLibrarySeriesLanguages
import eu.kanade.presentation.more.settings.screen.textrecognition.language.LibrarySeriesLanguages
import eu.kanade.presentation.more.settings.screen.textrecognition.playground.AndroidTextRecognitionPlaygroundImageDecoder
import eu.kanade.presentation.more.settings.screen.textrecognition.playground.TextRecognitionPlaygroundImageDecoder

@BindingContainer
interface TextRecognitionSettingsBindings {
    @Binds
    val AndroidTextRecognitionPlaygroundImageDecoder.playgroundImageDecoder: TextRecognitionPlaygroundImageDecoder

    @Binds
    val CatalogueLibrarySeriesLanguages.librarySeriesLanguages: LibrarySeriesLanguages
}
