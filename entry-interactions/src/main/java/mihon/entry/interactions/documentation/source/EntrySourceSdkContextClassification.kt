package mihon.entry.interactions.documentation.source

import mihon.feature.graph.ContextInputDefinition
import mihon.feature.graph.ContextInputId
import mihon.feature.graph.ContextInputMetadata
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.contextInputDefinition
import kotlin.reflect.KClass

val ENTRY_SOURCE_CONTEXT_OWNER = ContributionOwner("entry-source")
val ENTRY_SOURCE_DESCRIPTION_CONTEXT_OWNER = ContributionOwner("entry-source-description")
val ENTRY_SOURCE_SETTINGS_CONTEXT_OWNER = ContributionOwner("entry-source-settings")
val ENTRY_SOURCE_HOME_CONTEXT_OWNER = ContributionOwner("entry-source-home")

val ENTRY_SOURCE_CONTEXT_OWNERS = setOf(
    ENTRY_SOURCE_CONTEXT_OWNER,
    ENTRY_SOURCE_DESCRIPTION_CONTEXT_OWNER,
    ENTRY_SOURCE_SETTINGS_CONTEXT_OWNER,
    ENTRY_SOURCE_HOME_CONTEXT_OWNER,
)

sealed interface EntrySourceSdkContextClassification : ContextInputMetadata

data class EntrySourceSdkContractContext(
    val contract: KClass<*>,
    val integrations: Set<FeatureIntegrationId> = emptySet(),
) : EntrySourceSdkContextClassification {
    init {
        requireNotNull(contract.qualifiedName) { "Source SDK contract requires a qualified class name" }
    }
}

data class EntrySourceSdkContextExclusion(
    val reason: String,
    val integrations: Set<FeatureIntegrationId> = emptySet(),
) : EntrySourceSdkContextClassification {
    init {
        require(reason.isNotBlank()) { "Source context exclusion requires a reason" }
    }
}

inline fun <reified C : Any> entrySourceContextInputDefinition(
    id: ContextInputId,
    owner: ContributionOwner = ENTRY_SOURCE_CONTEXT_OWNER,
    contracts: Set<KClass<*>> = emptySet(),
    contractIntegrations: Map<KClass<*>, Set<FeatureIntegrationId>> = emptyMap(),
    nonContractReason: String? = null,
): ContextInputDefinition<C> {
    require((contracts.isNotEmpty()) xor (nonContractReason != null)) {
        "Source context $id must declare SDK contracts or one non-contract reason"
    }
    require(contractIntegrations.keys.all { it in contracts }) {
        "Source context $id scopes an integration for an undeclared SDK contract"
    }
    val metadata = contracts
        .mapTo(mutableSetOf<ContextInputMetadata>()) { contract ->
            EntrySourceSdkContractContext(contract, contractIntegrations[contract].orEmpty())
        }
        .apply {
            nonContractReason?.let { add(EntrySourceSdkContextExclusion(it)) }
        }
    return contextInputDefinition(id, owner, metadata)
}
