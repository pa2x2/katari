package app.katari.desktop.textrecognition

import androidx.compose.ui.unit.IntRect
import app.katari.core.image.argbPixels
import app.katari.core.image.asImageBitmap
import app.katari.core.image.decode.ImageRegionDecoder
import app.katari.core.image.pixelHeight
import app.katari.core.image.pixelWidth
import app.katari.text.recognition.api.image.ArgbPixels
import app.katari.text.recognition.api.image.ImageContentKey
import app.katari.text.recognition.ui.playground.PixelsTextRecognitionImage
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.textrecognition.playground.TextRecognitionPlaygroundImage
import eu.kanade.presentation.more.settings.screen.textrecognition.playground.TextRecognitionPlaygroundImageDecoder
import tachiyomi.domain.storage.DocumentUri
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

/**
 * Reads the plain file the desktop open dialog returns a `file://` URI of, with the decoder the readers use, so the
 * playground reads every format a page can have. Recognition reads the whole decoded picture.
 */
@Inject
class FileTextRecognitionPlaygroundImageDecoder : TextRecognitionPlaygroundImageDecoder {
    override fun decode(uri: DocumentUri): TextRecognitionPlaygroundImage? {
        val bytes = runCatching { Files.readAllBytes(Path.of(URI(uri.value))) }.getOrNull() ?: return null
        val key = ImageContentKey.sha256(bytes.inputStream())
        val bitmap = ImageRegionDecoder.open(bytes.inputStream())?.use { decoder ->
            var sampleSize = 1
            while (maxOf(decoder.width, decoder.height) / sampleSize > MAXIMUM_EDGE) sampleSize *= 2
            decoder.decode(IntRect(0, 0, decoder.width, decoder.height), sampleSize)
        } ?: return null
        val pixels = ArgbPixels(bitmap.pixelWidth, bitmap.pixelHeight, bitmap.argbPixels())
        return TextRecognitionPlaygroundImage(
            preview = bitmap.asImageBitmap(),
            recognition = PixelsTextRecognitionImage(pixels, key),
        )
    }

    private companion object {
        /** Keeps a tried image legible for recognition without holding a camera-sized bitmap. */
        const val MAXIMUM_EDGE = 2400
    }
}
