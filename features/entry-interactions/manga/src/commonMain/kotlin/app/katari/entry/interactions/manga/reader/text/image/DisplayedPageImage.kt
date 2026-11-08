package app.katari.entry.interactions.manga.reader.text.image

import app.katari.entry.interactions.manga.reader.text.geometry.MangaDisplayedPageGeometry
import app.katari.text.recognition.api.image.ImageContentKey
import app.katari.text.recognition.api.image.TextRecognitionImage

/** A displayed page image that holds decoding resources until it is closed. */
internal interface DisplayedPageImage : TextRecognitionImage, AutoCloseable {
    /** How this image relates to the raw page, for placing what was recognized on the raw page. */
    val geometry: MangaDisplayedPageGeometry

    /** Identity of the raw page file this image shows, as stored translations record it. */
    val rawContent: ImageContentKey
}
