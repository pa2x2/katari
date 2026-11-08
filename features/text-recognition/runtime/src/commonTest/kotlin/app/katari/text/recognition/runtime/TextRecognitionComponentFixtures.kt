package app.katari.text.recognition.runtime

import app.katari.language.api.tag.LanguageTag
import app.katari.model.artifacts.api.descriptor.ModelArtifactDescriptor
import app.katari.model.artifacts.api.descriptor.ModelArtifactFile
import app.katari.model.artifacts.api.descriptor.ModelArtifactHosting
import app.katari.model.artifacts.api.descriptor.ModelArtifactId
import app.katari.model.artifacts.api.descriptor.ModelArtifactLicense
import app.katari.text.recognition.api.component.KnownTextRecognitionComponent
import app.katari.text.recognition.api.component.TextRecognitionComponentId
import app.katari.text.recognition.api.component.TextRecognitionComponentRole
import app.katari.text.recognition.api.image.ImageContentKey
import app.katari.text.recognition.api.image.ImageRect
import app.katari.text.recognition.api.image.ImageSize
import app.katari.text.recognition.api.image.TextRecognitionBitmap
import app.katari.text.recognition.api.image.TextRecognitionImage
import app.katari.text.recognition.api.pipeline.TextRecognitionPipeline
import app.katari.text.recognition.api.pipeline.TextRecognitionPreset
import app.katari.text.recognition.api.pipeline.TextRecognitionPresetId
import app.katari.text.recognition.api.provider.KnownTextRecognitionProvider
import app.katari.text.recognition.api.provider.TextRecognitionBuildAvailability
import app.katari.text.recognition.api.provider.TextRecognitionProviderId
import app.katari.text.recognition.spi.component.DetectedTextRegion
import app.katari.text.recognition.spi.component.DetectedTextRegionKind
import app.katari.text.recognition.spi.component.RecognizedCropText
import app.katari.text.recognition.spi.component.TextDetector
import app.katari.text.recognition.spi.component.TextRecognitionComponent
import app.katari.text.recognition.spi.component.TextRecognitionComponentAvailability
import app.katari.text.recognition.spi.component.TextRecognizer
import app.katari.text.recognition.spi.contribution.TextRecognitionProviderContribution
import app.katari.text.recognition.spi.model.TextRecognitionModels
import io.mockk.mockk

internal val JAPANESE = LanguageTag.require("ja")

internal val EXAMPLE_PROVIDER = provider("example")
internal val EXCLUDED_PROVIDER = provider("excluded", TextRecognitionBuildAvailability.NotIncluded("Not in this build"))

internal fun provider(
    id: String,
    buildAvailability: TextRecognitionBuildAvailability = TextRecognitionBuildAvailability.Included,
) = KnownTextRecognitionProvider(
    id = TextRecognitionProviderId(id),
    name = id,
    description = "Example engine",
    processingLocation = "On this device",
    buildAvailability = buildAvailability,
)

internal fun knownComponent(
    id: String,
    role: TextRecognitionComponentRole,
    languages: Set<LanguageTag> = if (role == TextRecognitionComponentRole.Detector) emptySet() else setOf(JAPANESE),
    provider: KnownTextRecognitionProvider = EXAMPLE_PROVIDER,
) = KnownTextRecognitionComponent(
    id = TextRecognitionComponentId(id),
    provider = provider.id,
    role = role,
    displayName = id,
    description = "Example component",
    languages = languages,
)

internal fun preset(
    id: String,
    pipeline: TextRecognitionPipeline,
    languages: Set<LanguageTag> = setOf(JAPANESE),
    provider: KnownTextRecognitionProvider = EXAMPLE_PROVIDER,
) = TextRecognitionPreset(
    id = TextRecognitionPresetId(id),
    provider = provider.id,
    displayName = id,
    description = "Example preset",
    languages = languages,
    pipeline = pipeline,
)

/** A provider contribution that implements [implementations] and lists [catalogOnly] without implementing them. */
internal fun contribution(
    provider: KnownTextRecognitionProvider,
    implementations: List<TextRecognitionComponent> = emptyList(),
    catalogOnly: List<KnownTextRecognitionComponent> = emptyList(),
    presets: List<TextRecognitionPreset> = emptyList(),
    order: Int = 0,
) = TextRecognitionProviderContribution(
    provider = provider,
    components = implementations.map { it.catalogEntry } + catalogOnly,
    implementations = implementations,
    presets = presets,
    order = order,
)

internal fun model(id: String) = ModelArtifactDescriptor(
    id = ModelArtifactId(id),
    revision = "r1",
    displayName = id,
    files = listOf(ModelArtifactFile("model.onnx", "https://models.example/$id", 1, "0".repeat(64))),
    license = ModelArtifactLicense("Apache-2.0", "https://license.example"),
    hosting = ModelArtifactHosting.Upstream("https://models.example"),
)

/**
 * A page whose decoded regions remember where they came from, so fake components can answer in the coordinates of
 * the bitmap they receive.
 */
internal class FakePageImage(
    override val size: ImageSize,
    override val key: ImageContentKey = ImageContentKey("page"),
) : TextRecognitionImage {
    val decoded = mutableListOf<DecodedRegion>()

    override suspend fun decodeRegion(region: ImageRect, sampleSize: Int): TextRecognitionBitmap {
        val bitmap = mockk<TextRecognitionBitmap>(relaxed = true)
        decoded += DecodedRegion(bitmap, region, sampleSize)
        return bitmap
    }

    fun regionOf(bitmap: TextRecognitionBitmap): DecodedRegion = decoded.single { it.bitmap === bitmap }
}

internal data class DecodedRegion(
    val bitmap: TextRecognitionBitmap,
    val region: ImageRect,
    val sampleSize: Int,
)

/** Reports the page's ground-truth objects that intersect each tile, clipped to it like a real detector would. */
internal class FakeDetector(
    private val page: FakePageImage,
    private val objects: List<Pair<ImageRect, DetectedTextRegionKind>>,
    val declaredModels: List<ModelArtifactDescriptor> = emptyList(),
    override val catalogEntry: KnownTextRecognitionComponent =
        knownComponent("example.detector", TextRecognitionComponentRole.Detector),
) : TextDetector {
    override fun models(language: LanguageTag) = declaredModels

    override val inputEdge: Int = 640
    override var processingRevision = 1
    var runs = 0

    override suspend fun inspectDevice(language: LanguageTag) = TextRecognitionComponentAvailability.Available

    override suspend fun detect(tile: TextRecognitionBitmap, models: TextRecognitionModels): List<DetectedTextRegion> {
        runs++
        val decoded = page.regionOf(tile)
        return objects.mapNotNull { (bounds, kind) ->
            val visible = bounds.intersect(decoded.region) ?: return@mapNotNull null
            DetectedTextRegion(
                bounds = ImageRect(
                    left = (visible.left - decoded.region.left) / decoded.sampleSize,
                    top = (visible.top - decoded.region.top) / decoded.sampleSize,
                    right = (visible.right - decoded.region.left) / decoded.sampleSize,
                    bottom = (visible.bottom - decoded.region.top) / decoded.sampleSize,
                ),
                kind = kind,
                confidence = visible.area.toFloat() / bounds.area,
            )
        }
    }
}

/** Reads the text of whichever labelled source rectangle the crop covers most. */
internal class FakeRecognizer(
    private val page: FakePageImage,
    private val texts: Map<ImageRect, String>,
    val declaredModels: List<ModelArtifactDescriptor> = emptyList(),
    override val catalogEntry: KnownTextRecognitionComponent =
        knownComponent("example.recognizer", TextRecognitionComponentRole.Recognizer),
) : TextRecognizer {
    override fun models(language: LanguageTag) = declaredModels

    override val inputEdge: Int = 224
    override var processingRevision = 1
    var runs = 0

    override suspend fun inspectDevice(language: LanguageTag) = TextRecognitionComponentAvailability.Available

    override suspend fun recognize(
        crop: TextRecognitionBitmap,
        language: LanguageTag,
        models: TextRecognitionModels,
    ): RecognizedCropText? {
        runs++
        val region = page.regionOf(crop).region
        return texts.entries
            .maxByOrNull { (bounds, _) -> bounds.intersect(region)?.area ?: 0L }
            ?.takeIf { (bounds, _) -> bounds.intersect(region) != null }
            ?.let { RecognizedCropText(it.value) }
    }
}
