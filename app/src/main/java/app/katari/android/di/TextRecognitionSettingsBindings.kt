package app.katari.android.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.textrecognition.playground.AndroidTextRecognitionPlaygroundImageDecoder
import eu.kanade.presentation.more.settings.screen.textrecognition.playground.TextRecognitionPlaygroundImageDecoder

@BindingContainer
interface TextRecognitionSettingsBindings {
    @Binds
    val AndroidTextRecognitionPlaygroundImageDecoder.playgroundImageDecoder: TextRecognitionPlaygroundImageDecoder
}
