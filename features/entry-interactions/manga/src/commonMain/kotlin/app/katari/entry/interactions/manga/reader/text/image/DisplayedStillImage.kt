package app.katari.entry.interactions.manga.reader.text.image

import app.katari.text.recognition.api.image.ImageContentKey
import okio.ByteString

/**
 * A still page image as text features read it: [encoded] is the page file, decoded without its borders when
 * [cropBorders] says so, as the viewer decodes it. [rawContent] identifies the raw page, or is `null` when [encoded] is
 * the raw page itself.
 */
internal class DisplayedStillImage(
    val encoded: ByteString,
    val cropBorders: Boolean,
    val rawContent: ImageContentKey?,
)
