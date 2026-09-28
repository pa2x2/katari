package mihon.text.recognition.runtime.cache

import mihon.model.artifacts.api.descriptor.ModelArtifactId
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.image.ImageContentKey
import mihon.text.recognition.api.image.ImageSize
import mihon.text.recognition.spi.component.DetectedTextRegion

/**
 * The detections of recently read pages, kept in memory and evicting the least recently used page first.
 *
 * Detection depends only on the page and the detector, so an outline or another language on a page just read reuses
 * the page's detections. Pages are read one after another, so a few dozen entries cover the pages around the reader.
 */
internal class TextDetectionCache(
    private val maximumPages: Int,
) {
    private val entries = object : LinkedHashMap<TextDetectionCacheKey, List<DetectedTextRegion>>(
        maximumPages,
        LOAD_FACTOR,
        true,
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<TextDetectionCacheKey, List<DetectedTextRegion>>,
        ): Boolean = size > maximumPages
    }

    @Synchronized
    operator fun get(key: TextDetectionCacheKey): List<DetectedTextRegion>? = entries[key]

    @Synchronized
    operator fun set(key: TextDetectionCacheKey, detections: List<DetectedTextRegion>) {
        entries[key] = detections
    }

    private companion object {
        const val LOAD_FACTOR = 0.75f
    }
}

/**
 * Identity of one page's detections. The size is part of it because a host may present the same content at a
 * different resolution.
 */
internal data class TextDetectionCacheKey(
    val image: ImageContentKey,
    val size: ImageSize,
    val detector: TextRecognitionComponentId,
    val processingRevision: Int,
    val models: Map<ModelArtifactId, String>,
)
