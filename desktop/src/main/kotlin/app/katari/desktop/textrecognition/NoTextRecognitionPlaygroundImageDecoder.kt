package app.katari.desktop.textrecognition

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.textrecognition.playground.TextRecognitionPlaygroundImage
import eu.kanade.presentation.more.settings.screen.textrecognition.playground.TextRecognitionPlaygroundImageDecoder
import tachiyomi.domain.storage.DocumentUri

/**
 * Reads no picked image: recognition, which is all the playground decodes an image for, has no engine on desktop yet.
 * Stands in until desktop has a recognition engine.
 */
@Inject
class NoTextRecognitionPlaygroundImageDecoder : TextRecognitionPlaygroundImageDecoder {
    override fun decode(uri: DocumentUri): TextRecognitionPlaygroundImage? = null
}
