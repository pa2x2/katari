package app.katari.desktop.di

import app.katari.desktop.textrecognition.NoTextRecognitionPlaygroundImageDecoder
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.textrecognition.playground.TextRecognitionPlaygroundImageDecoder

@BindingContainer
interface TextRecognitionBindings {
    @Binds
    val NoTextRecognitionPlaygroundImageDecoder.playgroundImageDecoder: TextRecognitionPlaygroundImageDecoder
}
