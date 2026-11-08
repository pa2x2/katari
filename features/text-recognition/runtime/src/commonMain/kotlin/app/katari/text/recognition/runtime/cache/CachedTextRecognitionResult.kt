package app.katari.text.recognition.runtime.cache

import app.katari.text.recognition.api.image.ImageRect
import app.katari.text.recognition.api.result.RecognizedTextRegion
import app.katari.text.recognition.api.result.TextOrientation
import app.katari.text.recognition.api.result.TextRegionKind
import kotlinx.serialization.Serializable

/** Persisted form of recognized regions. */
@Serializable
internal data class CachedTextRecognitionResult(
    val regions: List<CachedTextRegion>,
) {
    fun toRegions(): List<RecognizedTextRegion> = regions.map { region ->
        RecognizedTextRegion(
            bounds = region.bounds.toRect(),
            text = region.text,
            kind = TextRegionKind.valueOf(region.kind),
            orientation = TextOrientation.valueOf(region.orientation),
            container = region.container?.toRect(),
        )
    }
}

@Serializable
internal data class CachedTextRegion(
    val bounds: CachedRect,
    val text: String,
    val kind: String,
    val orientation: String,
    val container: CachedRect? = null,
)

@Serializable
internal data class CachedRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    fun toRect(): ImageRect = ImageRect(left, top, right, bottom)
}

internal fun List<RecognizedTextRegion>.toCached(): CachedTextRecognitionResult = CachedTextRecognitionResult(
    regions = map { region ->
        CachedTextRegion(
            bounds = region.bounds.toCached(),
            text = region.text,
            kind = region.kind.name,
            orientation = region.orientation.name,
            container = region.container?.toCached(),
        )
    },
)

private fun ImageRect.toCached(): CachedRect = CachedRect(left, top, right, bottom)
