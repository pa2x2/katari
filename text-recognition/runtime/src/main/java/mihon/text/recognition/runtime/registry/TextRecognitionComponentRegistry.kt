package mihon.text.recognition.runtime.registry

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.component.TextRecognitionComponentRole
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPreset
import mihon.text.recognition.api.pipeline.TextRecognitionPresetId
import mihon.text.recognition.api.provider.KnownTextRecognitionProvider
import mihon.text.recognition.api.provider.TextRecognitionBuildAvailability
import mihon.text.recognition.api.provider.TextRecognitionProviderId
import mihon.text.recognition.runtime.language.readsLanguage
import mihon.text.recognition.runtime.language.recognitionLanguage
import mihon.text.recognition.spi.component.TextDetector
import mihon.text.recognition.spi.component.TextRecognitionComponent
import mihon.text.recognition.spi.component.TextRecognitionEngine
import mihon.text.recognition.spi.component.TextRecognizer
import mihon.text.recognition.spi.contribution.TextRecognitionProviderContribution

/**
 * Providers, their components, and their presets in this build, validated together so that every preset names
 * components of the right roles that read its languages.
 */
internal class TextRecognitionComponentRegistry(
    contributions: List<TextRecognitionProviderContribution>,
) {
    private val ordered = contributions.sortedWith(
        compareBy(TextRecognitionProviderContribution::order, { it.provider.id.value }),
    )

    val providers: List<KnownTextRecognitionProvider> = ordered.map(TextRecognitionProviderContribution::provider)
    val knownComponents: List<KnownTextRecognitionComponent> = ordered.flatMap(
        TextRecognitionProviderContribution::components,
    )
    val presets: List<TextRecognitionPreset> = ordered.flatMap(TextRecognitionProviderContribution::presets)

    private val providersById = providers.associateBy(KnownTextRecognitionProvider::id)
    private val catalogById = knownComponents.associateBy(KnownTextRecognitionComponent::id)
    private val componentsById: Map<TextRecognitionComponentId, TextRecognitionComponent> =
        ordered.flatMap(TextRecognitionProviderContribution::implementations).associateBy { it.catalogEntry.id }
    private val presetsById = presets.associateBy(TextRecognitionPreset::id)

    /** Providers this build can run, in catalog order. */
    val includedProviders: List<KnownTextRecognitionProvider> =
        providers.filter { it.buildAvailability == TextRecognitionBuildAvailability.Included }

    /** Languages at least one component of this build reads, in catalog order. */
    val supportedLanguages: List<LanguageTag> = knownComponents
        .filter { it.id in componentsById }
        .flatMap(KnownTextRecognitionComponent::languages)
        .distinctBy { it.recognitionLanguage }

    init {
        requireUnique(providers.map { it.id.value }, "providers")
        requireUnique(knownComponents.map { it.id.value }, "components")
        requireUnique(presets.map { it.id.value }, "presets")
        componentsById.values.forEach { component ->
            require(component.implementedRole == component.catalogEntry.role) {
                "Text recognition component ${component.catalogEntry.id.value} does not implement its declared role"
            }
        }
        presets.forEach { preset ->
            require(isWellFormed(preset.pipeline)) {
                "Text recognition preset ${preset.id.value} names unknown components or wrong roles"
            }
            preset.languages.forEach { language ->
                require(reads(preset.pipeline, language)) {
                    "Text recognition preset ${preset.id.value} cannot read ${language.value}"
                }
            }
        }
    }

    fun provider(id: TextRecognitionProviderId): KnownTextRecognitionProvider? = providersById[id]

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

    private fun requireUnique(ids: List<String>, kind: String) {
        val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        require(duplicates.isEmpty()) { "Duplicate text recognition $kind: ${duplicates.sorted()}" }
    }
}

private val TextRecognitionComponent.implementedRole: TextRecognitionComponentRole
    get() = when (this) {
        is TextDetector -> TextRecognitionComponentRole.Detector
        is TextRecognizer -> TextRecognitionComponentRole.Recognizer
        is TextRecognitionEngine -> TextRecognitionComponentRole.Engine
    }
