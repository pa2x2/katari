package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.graph.CapabilityId
import mihon.feature.graph.CapabilityProvider
import mihon.feature.graph.ContentTypeContribution
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.ContractFixture
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.SpecializedAdapter
import mihon.feature.graph.capabilityDefinition
import tachiyomi.domain.entry.model.Entry

interface EntryInteractionSpecializedAdapter {
    val type: EntryType
}

interface EntryInteractionProvider {
    val type: EntryType
}

open class EntryInteractionCapability<P : EntryInteractionProvider> internal constructor(
    val definition: mihon.feature.graph.CapabilityDefinition<P>,
) {
    fun bind(implementation: P): EntryInteractionProviderBinding<P> {
        return EntryInteractionProviderBinding(this, implementation)
    }
}

class EntryInteractionProviderBinding<P : EntryInteractionProvider> internal constructor(
    internal val capability: EntryInteractionCapability<P>,
    val implementation: P,
) {
    val graphProvider: CapabilityProvider<P> = CapabilityProvider(capability.definition, implementation)
}

internal inline fun <reified P : EntryInteractionProvider> entryInteractionCapability(
    id: CapabilityId,
): EntryInteractionCapability<P> {
    return EntryInteractionCapability(
        definition = capabilityDefinition(id, ENTRY_INTERACTION_CONTRACT_OWNER),
    )
}

internal val ENTRY_INTERACTION_CONTRACT_OWNER = ContributionOwner("entry-interactions")

fun EntryType.toContentTypeId(): ContentTypeId = ContentTypeId(name.lowercase())

interface EntryInteractionPlugin : FeatureGraphContributor {
    val type: EntryType
    override val owner: ContributionOwner
    val providerBindings: List<EntryInteractionProviderBinding<*>>
    val specializedAdapters: List<SpecializedAdapter<*>>
        get() = emptyList()
    val contractFixtures: List<ContractFixture<*>>
        get() = emptyList()

    val contentTypeContribution: ContentTypeContribution
        get() = ContentTypeContribution(
            contentType = type.toContentTypeId(),
            owner = owner,
            providers = providerBindings.map { it.graphProvider },
            specializedAdapters = specializedAdapters,
            contractFixtures = contractFixtures,
        )

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(contentTypeContribution)
    }

    fun validateContribution() {
        require(contentTypeContribution.contentType == type.toContentTypeId()) {
            "Entry interaction plugin $type must contribute content type ${type.toContentTypeId()}, not " +
                contentTypeContribution.contentType
        }
        providerBindings.forEach { binding ->
            require(binding.implementation.type == type) {
                "Entry interaction plugin $type cannot contribute ${binding.graphProvider.capability.id} for " +
                    binding.implementation.type
            }
        }
        specializedAdapters.forEach { adapter ->
            val implementation = adapter.implementation
            require(implementation is EntryInteractionSpecializedAdapter) {
                "Entry interaction specialized adapter ${adapter.definition.id} must expose its Entry type"
            }
            require(implementation.type == type) {
                "Entry interaction plugin $type cannot contribute specialized adapter ${adapter.definition.id} for " +
                    implementation.type
            }
        }
    }
}
