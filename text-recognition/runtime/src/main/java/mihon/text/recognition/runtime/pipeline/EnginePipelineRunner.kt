package mihon.text.recognition.runtime.pipeline

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.TextRecognitionImage
import mihon.text.recognition.api.result.RecognizedTextRegion
import mihon.text.recognition.api.result.TextRegionKind
import mihon.text.recognition.runtime.geometry.mergeOverlapping
import mihon.text.recognition.runtime.geometry.planTiles
import mihon.text.recognition.runtime.geometry.sampleSizeForMaximumEdge
import mihon.text.recognition.runtime.geometry.toSource
import mihon.text.recognition.spi.component.TextRecognitionEngine
import mihon.text.recognition.spi.model.TextRecognitionModels

/** Runs a combined engine over tiles of the area and removes regions that overlapping tiles read twice. */
internal class EnginePipelineRunner(
    private val engine: TextRecognitionEngine,
) : TextRecognitionPipelineRunner {

    override suspend fun run(
        image: TextRecognitionImage,
        area: ImageRect,
        outlinedByUser: Boolean,
        language: LanguageTag,
        models: TextRecognitionModels,
    ): List<RecognizedTextRegion> {
        val regions = planTiles(area, minimumLength = engine.maximumInputEdge).flatMap { tile ->
            val sampleSize = sampleSizeForMaximumEdge(tile, engine.maximumInputEdge)
            val bitmap = image.decodeRegion(tile, sampleSize)
            try {
                engine.recognize(bitmap, language, models).mapNotNull { region ->
                    val bounds = region.bounds.toSource(tile, sampleSize) ?: return@mapNotNull null
                    val text = region.text.trim().takeIf(String::isNotEmpty) ?: return@mapNotNull null
                    RecognizedTextRegion(
                        bounds = bounds,
                        text = text,
                        kind = TextRegionKind.Unclassified,
                        orientation = region.orientation,
                    )
                }
            } finally {
                bitmap.recycle()
            }
        }
        // A region read twice keeps the longer reading: the shorter one was truncated at a tile edge.
        return mergeOverlapping(
            candidates = regions,
            bounds = RecognizedTextRegion::bounds,
            withBounds = { region, bounds -> region.copy(bounds = bounds) },
            priority = compareByDescending { it.text.length },
        )
    }
}
