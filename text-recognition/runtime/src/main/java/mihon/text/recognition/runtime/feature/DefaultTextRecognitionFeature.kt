package mihon.text.recognition.runtime.feature

import kotlinx.coroutines.CancellationException
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.model.artifacts.api.state.InstalledModelArtifact
import mihon.text.recognition.api.TextRecognitionFeature
import mihon.text.recognition.api.configuration.TextRecognitionPipelineResolution
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.preparation.ReadyTextRecognition
import mihon.text.recognition.api.preparation.TextRecognitionPipelineChoiceReason
import mihon.text.recognition.api.preparation.TextRecognitionPreparation
import mihon.text.recognition.api.preparation.TextRecognitionUnavailableReason
import mihon.text.recognition.api.request.TextRecognitionRequest
import mihon.text.recognition.api.request.TextRecognitionScope
import mihon.text.recognition.api.result.RecognizedTextRegion
import mihon.text.recognition.api.result.TextRecognitionExecution
import mihon.text.recognition.api.result.TextRecognitionResult
import mihon.text.recognition.runtime.cache.TextRecognitionCacheKey
import mihon.text.recognition.runtime.execution.CachedRecognitionExecutor
import mihon.text.recognition.runtime.geometry.inReadingOrder
import mihon.text.recognition.runtime.language.readsRightToLeft
import mihon.text.recognition.runtime.language.recognitionLanguage
import mihon.text.recognition.runtime.pipeline.EnginePipelineRunner
import mihon.text.recognition.runtime.pipeline.StagedPipelineRunner
import mihon.text.recognition.runtime.pipeline.TextRecognitionPipelineRunner
import mihon.text.recognition.runtime.registry.TextRecognitionComponentRegistry
import mihon.text.recognition.runtime.selection.ProfileTextRecognitionPreferences
import mihon.text.recognition.runtime.selection.TextRecognitionPipelineResolver
import mihon.text.recognition.spi.component.TextDetector
import mihon.text.recognition.spi.component.TextRecognitionComponent
import mihon.text.recognition.spi.component.TextRecognitionComponentAvailability
import mihon.text.recognition.spi.component.TextRecognitionEngine
import mihon.text.recognition.spi.component.TextRecognizer
import mihon.text.recognition.spi.model.TextRecognitionModels

internal class DefaultTextRecognitionFeature(
    private val registry: TextRecognitionComponentRegistry,
    private val resolver: TextRecognitionPipelineResolver,
    private val preferences: ProfileTextRecognitionPreferences,
    private val modelStore: ModelArtifactStore,
    private val executor: CachedRecognitionExecutor,
) : TextRecognitionFeature {

    override suspend fun prepare(request: TextRecognitionRequest): TextRecognitionPreparation {
        val language = request.language
            ?: return TextRecognitionPreparation.LanguageRequired(registry.supportedLanguages)
        val pipeline = request.pipeline
            ?.also { explicit ->
                if (!registry.isWellFormed(explicit) || !registry.reads(explicit, language)) {
                    return choiceRequired(language, TextRecognitionPipelineChoiceReason.NothingSelected)
                }
            }
            ?: when (val resolution = resolver.resolve(preferences.configuration(), language)) {
                is TextRecognitionPipelineResolution.Resolved -> resolution.pipeline
                is TextRecognitionPipelineResolution.OverrideUnavailable -> {
                    val missing = resolver.pipeline(resolution.selection)?.components
                        ?.firstOrNull { registry.component(it) == null }
                    return choiceRequired(
                        language,
                        missing?.let(TextRecognitionPipelineChoiceReason::SelectedComponentUnavailable)
                            ?: TextRecognitionPipelineChoiceReason.NothingSelected,
                    )
                }
                TextRecognitionPipelineResolution.ChoiceRequired ->
                    return choiceRequired(language, TextRecognitionPipelineChoiceReason.NothingSelected)
                TextRecognitionPipelineResolution.UnsupportedLanguage -> return TextRecognitionPreparation.Unavailable(
                    TextRecognitionUnavailableReason.UnsupportedLanguage(language),
                )
            }
        val components = pipeline.components.map { id ->
            registry.component(id) ?: return choiceRequired(
                language,
                TextRecognitionPipelineChoiceReason.SelectedComponentUnavailable(id),
            )
        }
        components.forEach { component ->
            val availability = component.inspectDevice()
            if (availability is TextRecognitionComponentAvailability.Unavailable) {
                return TextRecognitionPreparation.Unavailable(
                    TextRecognitionUnavailableReason.ComponentUnavailable(
                        component.catalogEntry.id,
                        availability.reason,
                    ),
                )
            }
        }
        val missingModels = components.flatMap { it.models(language) }.distinct()
            .filter { modelStore.installed(it) == null }
        if (missingModels.isNotEmpty()) {
            return TextRecognitionPreparation.ModelsRequired(language, pipeline, missingModels)
        }
        return TextRecognitionPreparation.Ready(
            recognition = PreparedRecognition(request, language, pipeline, components),
            language = language,
            pipeline = pipeline,
        )
    }

    private fun choiceRequired(language: LanguageTag, reason: TextRecognitionPipelineChoiceReason) =
        TextRecognitionPreparation.PipelineChoiceRequired(language, reason, registry.presets(language))

    override suspend fun recognize(ready: ReadyTextRecognition): TextRecognitionExecution {
        val prepared = ready as? PreparedRecognition
            ?: return TextRecognitionExecution.Failed("Recognition was not prepared by this feature")
        val installed = mutableListOf<InstalledModelArtifact>()
        prepared.components.flatMap { it.models(prepared.language) }.distinct().forEach { model ->
            installed += modelStore.installed(model)
                ?: return TextRecognitionExecution.PreparationChanged(prepare(prepared.request))
        }
        val image = prepared.request.image
        val area = when (val scope = prepared.request.scope) {
            TextRecognitionScope.WholeImage -> image.size.bounds
            is TextRecognitionScope.Region -> scope.region.intersect(image.size.bounds)
                ?: return TextRecognitionExecution.Success(result(prepared, emptyList()))
        }
        return try {
            val regions = executor.execute(cacheKey(prepared, area, installed)) {
                prepared.runner().run(
                    image = image,
                    area = area,
                    outlinedByUser = prepared.request.scope is TextRecognitionScope.Region,
                    language = prepared.language,
                    models = TextRecognitionModels(installed),
                )
            }
            val ordered = inReadingOrder(
                elements = regions,
                bounds = { it.container ?: it.bounds },
                rightToLeft = prepared.language.readsRightToLeft,
            )
            TextRecognitionExecution.Success(result(prepared, ordered))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            TextRecognitionExecution.Failed(error.message)
        }
    }

    private fun result(prepared: PreparedRecognition, regions: List<RecognizedTextRegion>) =
        TextRecognitionResult(
            image = prepared.request.image.key,
            imageSize = prepared.request.image.size,
            language = prepared.language,
            regions = regions,
        )

    private fun cacheKey(
        prepared: PreparedRecognition,
        area: ImageRect,
        installed: List<InstalledModelArtifact>,
    ): TextRecognitionCacheKey = TextRecognitionCacheKey(
        buildList {
            add(CACHE_FORMAT)
            add(prepared.request.image.key.value)
            add("${area.left},${area.top},${area.right},${area.bottom}")
            add(prepared.request.scope::class.simpleName.orEmpty())
            add(prepared.language.recognitionLanguage)
            prepared.pipeline.components.forEach { add(it.value) }
            installed.sortedBy { it.descriptor.id.value }.forEach { model ->
                add("${model.descriptor.id.value}@${model.descriptor.revision}")
            }
        },
    )

    private fun PreparedRecognition.runner(): TextRecognitionPipelineRunner = when (pipeline) {
        is TextRecognitionPipeline.Staged -> StagedPipelineRunner(
            detector = components[0] as TextDetector,
            recognizer = components[1] as TextRecognizer,
        )
        is TextRecognitionPipeline.Engine -> EnginePipelineRunner(components.single() as TextRecognitionEngine)
    }

    private class PreparedRecognition(
        val request: TextRecognitionRequest,
        val language: LanguageTag,
        val pipeline: TextRecognitionPipeline,
        val components: List<TextRecognitionComponent>,
    ) : ReadyTextRecognition

    private companion object {
        /** Bump when the runtime's own processing changes in a way that invalidates cached results. */
        const val CACHE_FORMAT = "text-recognition-1"
    }
}
