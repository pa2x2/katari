package mihon.entry.interactions.documentation

import mihon.feature.graph.CapabilityProvider
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.ContextEvidence
import mihon.feature.graph.FeatureIntegrationSubject
import mihon.feature.graph.SpecializedAdapter

enum class EntryContentTypeReferenceSection(
    val title: String,
    val order: Int,
) {
    ENTRY_INTERACTIONS("Entry interactions", 100),
    LIBRARY_AND_UPDATES("Library and updates", 200),
    DOWNLOADS("Downloads", 300),
    DISCOVERY_AND_INTEGRATIONS("Discovery and integrations", 400),
}

sealed interface EntryContentTypeReferenceElement {
    val id: String
    val section: EntryContentTypeReferenceSection
    val order: Int
}

data class EntryContentTypeReferenceRow(
    override val id: String,
    override val section: EntryContentTypeReferenceSection,
    val label: String,
    override val order: Int,
) : EntryContentTypeReferenceElement {
    init {
        require(id.isNotBlank()) { "Content-type reference row requires an ID" }
        require(label.isNotBlank()) { "Content-type reference row $id requires a label" }
    }
}

data class EntryContentTypeReferenceNote(
    override val id: String,
    override val section: EntryContentTypeReferenceSection,
    val text: String,
    override val order: Int,
) : EntryContentTypeReferenceElement {
    init {
        require(id.isNotBlank()) { "Content-type reference note requires an ID" }
        require(text.isNotBlank()) { "Content-type reference note $id requires text" }
    }
}

enum class EntryContentTypeReferenceStatus {
    SUPPORTED,
    SOURCE_DEPENDENT,
}

/** Which evaluated relationship state is factual for this public projection. */
enum class EntryContentTypeReferenceSelection {
    /** Static applicability or authoritative resolved context establishes the cell. */
    APPLICABLE_RELATIONSHIP,

    /** The existence of a conditional relationship establishes only a source-dependent cell. */
    CONDITIONAL_RELATIONSHIP,
}

sealed interface EntryContentTypeReferenceProjectionResult {
    data class Cell(
        val status: EntryContentTypeReferenceStatus,
    ) : EntryContentTypeReferenceProjectionResult

    data object IncludedNote : EntryContentTypeReferenceProjectionResult
}

data class EntryContentTypeReferenceProjectionInput(
    val subject: FeatureIntegrationSubject,
    val matchedProviders: List<CapabilityProvider<*>>,
    val suppliedAdapters: List<SpecializedAdapter<*>>,
    val contextEvidence: List<ContextEvidence<*>>,
) {
    val contentType: ContentTypeId
        get() = subject.contentType

    inline fun <reified P : Any> requireMatchedProvider(): P {
        return matchedProviders
            .map(CapabilityProvider<*>::implementation)
            .filterIsInstance<P>()
            .singleOrNull()
            ?: error("${subject.feature} projection requires one matched ${P::class.qualifiedName} provider")
    }
}

interface EntryContentTypeReferenceProjection {
    val element: EntryContentTypeReferenceElement
    val selection: EntryContentTypeReferenceSelection
        get() = EntryContentTypeReferenceSelection.APPLICABLE_RELATIONSHIP

    fun project(input: EntryContentTypeReferenceProjectionInput): EntryContentTypeReferenceProjectionResult
}
