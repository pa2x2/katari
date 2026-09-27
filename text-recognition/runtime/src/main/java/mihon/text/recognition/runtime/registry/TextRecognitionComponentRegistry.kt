package mihon.text.recognition.runtime.registry

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.component.TextRecognitionBuildAvailability
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.component.TextRecognitionComponentRole
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPreset
import mihon.text.recognition.api.pipeline.TextRecognitionPresetId
import mihon.text.recognition.runtime.language.readsLanguage
import mihon.text.recognition.runtime.language.recognitionLanguage
import mihon.text.recognition.spi.component.TextDetector
import mihon.text.recognition.spi.component.TextRecognitionComponent
import mihon.text.recognition.spi.component.TextRecognitionEngine
import mihon.text.recognition.spi.component.TextRecognizer
import mihon.text.recognition.spi.contribution.TextRecognitionComponentContribution
import mihon.text.recognition.spi.contribution.TextRecognitionPresetContribution

/**
 * Catalog, executable components, and presets of this build, validated together so that every preset names
 * components of the right roles that read its languages.
 */
internal class TextRecognitionComponentRegistry(
    contributions: List<TextRecognitionComponentContribution>,
    presetContributions: List<TextRecognitionPresetContribution>,
) {
    val knownComponents: List<KnownTextRecognitionComponent> = contributions
        .sortedWith(compareBy(TextRecognitionComponentContribution::order, { it.catalogEntry.id.value }))
        .map(TextRecognitionComponentContribution::catalogEntry)
    val presets: List<TextRecognitionPreset> = presetContributions
        .sortedWith(compareBy(TextRecognitionPresetContribution::order, { it.preset.id.value }))
        .map(TextRecognitionPresetContribution::preset)

    private val catalogById = knownComponents.associateBy(KnownTextRecognitionComponent::id)
    private val componentsById: Map<TextRecognitionComponentId, TextRecognitionComponent> =
        contributions.mapNotNull { it.component }.associateBy { it.catalogEntry.id }
    private val presetsById = presets.associateBy(TextRecognitionPreset::id)

    /** Languages at least one component of this build reads, ordered by their first appearance in the catalog. */
    val supportedLanguages: List<LanguageTag> = knownComponents
        .filter { it.buildAvailability == TextRecognitionBuildAvailability.Included }
        .flatMap(KnownTextRecognitionComponent::languages)
        .distinctBy { it.recognitionLanguage }

    init {
        val duplicateComponents = contributions.groupingBy { it.catalogEntry.id }.eachCount().filterValues { it > 1 }
        require(duplicateComponents.isEmpty()) {
            "Duplicate text recognition components: ${duplicateComponents.keys.map { it.value }.sorted()}"
        }
        val duplicatePresets = presetContributions.groupingBy { it.preset.id }.eachCount().filterValues { it > 1 }
        require(duplicatePresets.isEmpty()) {
            "Duplicate text recognition presets: ${duplicatePresets.keys.map { it.value }.sorted()}"
        }
        componentsById.values.forEach { component ->
            require(component.implementedRole == component.catalogEntry.role) {
                "Text recognition component ${component.catalogEntry.id.value} does not implement its declared role"
            }
        }
        presets.forEach { preset ->
            requireValidPipeline(preset.pipeline, preset.languages, "preset ${preset.id.value}")
        }
    }

    fun component(id: TextRecognitionComponentId): TextRecognitionComponent? = componentsById[id]

    fun catalogEntry(id: TextRecognitionComponentId): KnownTextRecognitionComponent? = catalogById[id]

    fun preset(id: TextRecognitionPresetId): TextRecognitionPreset? = presetsById[id]

    fun presets(language: LanguageTag): List<TextRecognitionPreset> =
        presets.filter { it.languages.readsLanguage(language) }

    /** Every well-formed pipeline whose reader supports [language], in catalog order. */
    fun pipelines(language: LanguageTag): List<TextRecognitionPipeline> {
        fun readers(role: TextRecognitionComponentRole) =
            knownComponents.filter { it.role == role && it.languages.readsLanguage(language) }
        val detectors = knownComponents.filter { it.role == TextRecognitionComponentRole.Detector }
        val staged = detectors.flatMap { detector ->
            readers(TextRecognitionComponentRole.Recognizer).map { recognizer ->
                TextRecognitionPipeline.Staged(detector.id, recognizer.id)
            }
        }
        return staged + readers(TextRecognitionComponentRole.Engine).map { TextRecognitionPipeline.Engine(it.id) }
    }

    /** Whether every component of [pipeline] is in the catalog with the role its position requires. */
    fun isWellFormed(pipeline: TextRecognitionPipeline): Boolean = when (pipeline) {
        is TextRecognitionPipeline.Staged ->
            catalogById[pipeline.detector]?.role == TextRecognitionComponentRole.Detector &&
                catalogById[pipeline.recognizer]?.role == TextRecognitionComponentRole.Recognizer
        is TextRecognitionPipeline.Engine ->
            catalogById[pipeline.engine]?.role == TextRecognitionComponentRole.Engine
    }

    /** Whether the component of [pipeline] that reads text supports [language]. */
    fun reads(pipeline: TextRecognitionPipeline, language: LanguageTag): Boolean {
        val reader = when (pipeline) {
            is TextRecognitionPipeline.Staged -> pipeline.recognizer
            is TextRecognitionPipeline.Engine -> pipeline.engine
        }
        return catalogById[reader]?.languages?.readsLanguage(language) == true
    }

    /** Whether every component of [pipeline] can execute in this build. */
    fun isIncluded(pipeline: TextRecognitionPipeline): Boolean = pipeline.components.all(componentsById::containsKey)

    private fun requireValidPipeline(pipeline: TextRecognitionPipeline, languages: Set<LanguageTag>, owner: String) {
        require(isWellFormed(pipeline)) { "Text recognition $owner names unknown components or wrong roles" }
        languages.forEach { language ->
            require(reads(pipeline, language)) { "Text recognition $owner cannot read ${language.value}" }
        }
    }
}

private val TextRecognitionComponent.implementedRole: TextRecognitionComponentRole
    get() = when (this) {
        is TextDetector -> TextRecognitionComponentRole.Detector
        is TextRecognizer -> TextRecognitionComponentRole.Recognizer
        is TextRecognitionEngine -> TextRecognitionComponentRole.Engine
    }
