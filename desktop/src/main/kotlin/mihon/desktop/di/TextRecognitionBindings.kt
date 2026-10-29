package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.textrecognition.playground.TextRecognitionPlaygroundImageDecoder
import mihon.desktop.textrecognition.NoTextRecognitionPlaygroundImageDecoder

@BindingContainer
interface TextRecognitionBindings {
    @Binds
    val NoTextRecognitionPlaygroundImageDecoder.playgroundImageDecoder: TextRecognitionPlaygroundImageDecoder
}
