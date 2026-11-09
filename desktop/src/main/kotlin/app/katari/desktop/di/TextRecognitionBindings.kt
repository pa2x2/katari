package app.katari.desktop.di

import app.katari.desktop.textrecognition.FileTextRecognitionPlaygroundImageDecoder
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.textrecognition.playground.TextRecognitionPlaygroundImageDecoder

@BindingContainer
interface TextRecognitionBindings {
    @Binds
    val FileTextRecognitionPlaygroundImageDecoder.playgroundImageDecoder: TextRecognitionPlaygroundImageDecoder
}
