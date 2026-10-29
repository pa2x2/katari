package mihon.entry.interactions.manga.reader.text.image

import mihon.entry.interactions.manga.reader.text.geometry.MangaPageTransform
import mihon.text.recognition.api.image.ImageContentKey
import okio.ByteString

/**
 * A still image a page view shows: [encoded] is what the view decodes, derived from the raw page by [transform], and
 * [cropBorders] is whether the view crops its borders. [rawContent] identifies the raw page, or is `null` when
 * [encoded] is the raw page itself.
 */
internal class DisplayedStillImage(
    val encoded: ByteString,
    val cropBorders: Boolean,
    val transform: MangaPageTransform,
    val rawContent: ImageContentKey?,
)
