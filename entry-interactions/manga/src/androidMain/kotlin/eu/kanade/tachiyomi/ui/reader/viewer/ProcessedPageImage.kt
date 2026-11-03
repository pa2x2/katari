package eu.kanade.tachiyomi.ui.reader.viewer

import mihon.entry.interactions.manga.reader.text.geometry.MangaPageTransform
import mihon.text.recognition.api.image.ImageContentKey
import mihon.text.recognition.api.image.ImageSize
import okio.BufferedSource
import tachiyomi.core.common.util.system.ImageUtil

/**
 * A page image as a viewer prepared it for display, and how that image was derived from the raw page. [rawContent]
 * identifies the raw page when [source] is not the raw page itself.
 */
internal class ProcessedPageImage(
    val source: BufferedSource,
    val transform: MangaPageTransform = MangaPageTransform.None,
    val rawContent: ImageContentKey? = null,
)

/**
 * Derives the displayed image from the raw page [raw] with [derive], which consumes [raw]; the raw page's size and
 * identity are read first. [transform] describes the derivation for a raw page of the given size.
 */
internal inline fun transformedPageImage(
    raw: BufferedSource,
    transform: (ImageSize) -> MangaPageTransform,
    derive: (BufferedSource) -> BufferedSource,
): ProcessedPageImage {
    val size = ImageUtil.imageSize(raw).let { ImageSize(it.width, it.height) }
    val content = ImageContentKey.sha256(raw.peek().inputStream())
    return ProcessedPageImage(derive(raw), transform(size), content)
}
