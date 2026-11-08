package mihon.entry.interactions.manga.reader.text.image

import mihon.entry.interactions.manga.reader.text.geometry.MangaDisplayedPageGeometry
import mihon.text.recognition.api.image.ImageContentKey
import mihon.text.recognition.api.image.TextRecognitionImage

/** A displayed page image that holds decoding resources until it is closed. */
internal interface DisplayedPageImage : TextRecognitionImage, AutoCloseable {
    /** How this image relates to the raw page, for placing what was recognized on the raw page. */
    val geometry: MangaDisplayedPageGeometry

    /** Identity of the raw page file this image shows, as stored translations record it. */
    val rawContent: ImageContentKey
}
