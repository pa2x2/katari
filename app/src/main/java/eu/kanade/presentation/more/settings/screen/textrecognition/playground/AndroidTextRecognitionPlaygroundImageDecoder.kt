package eu.kanade.presentation.more.settings.screen.textrecognition.playground

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import dev.zacsweers.metro.Inject
import mihon.text.recognition.api.image.ImageContentKey
import mihon.text.recognition.ui.playground.BitmapTextRecognitionImage
import tachiyomi.domain.storage.DocumentUri

/** Reads the picked content once for its key and decodes it into a software bitmap, which recognition reads whole. */
@Inject
class AndroidTextRecognitionPlaygroundImageDecoder(
    private val application: Application,
) : TextRecognitionPlaygroundImageDecoder {
    override fun decode(uri: DocumentUri): TextRecognitionPlaygroundImage? {
        val contentUri = uri.value.toUri()
        val key = application.contentResolver.openInputStream(contentUri)?.use(ImageContentKey::sha256) ?: return null
        val bitmap = decodeBitmap(contentUri)
        return TextRecognitionPlaygroundImage(
            preview = bitmap.asImageBitmap(),
            recognition = BitmapTextRecognitionImage(bitmap, key),
        )
    }

    private fun decodeBitmap(uri: Uri): Bitmap =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(application.contentResolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > MAXIMUM_EDGE) {
                val scale = MAXIMUM_EDGE.toFloat() / longest
                decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
            }
        }

    private companion object {
        /** Keeps a tried image legible for recognition without holding a camera-sized bitmap. */
        const val MAXIMUM_EDGE = 2400
    }
}
