package mihon.entry.interactions.documentation.source

import mihon.feature.graph.FeatureGraph

fun planEntrySourceSdkConsumerCoverage(graph: FeatureGraph): EntrySourceSdkConsumerCoveragePlan {
    val issues = mutableListOf<EntrySourceSdkConsumerCoverageIssue>()
    val exclusions = mutableListOf<EntrySourceSdkContextExclusionRecord>()
    val consumers = mutableListOf<EntrySourceSdkContextConsumer>()
    graph.features.forEach { feature ->
        feature.integrations.forEach { integration ->
            integration.contextInputs.forEach { input ->
                val classifications = input.metadata
                    .filterIsInstance<EntrySourceSdkContextClassification>()
                    .filter { classification -> classification.appliesTo(integration.id) }
                val contracts = classifications.filterIsInstance<EntrySourceSdkContractContext>()
                val declaredExclusions = classifications.filterIsInstance<EntrySourceSdkContextExclusion>()
                if (input.owner in ENTRY_SOURCE_CONTEXT_OWNERS && classifications.isEmpty()) {
                    issues += EntrySourceSdkConsumerCoverageIssue(
                        responsibleOwner = input.owner,
                        contextInput = input.id,
                        details = "Source-owned context input ${input.id.value} does not classify SDK contract " +
                            "coverage for integration ${integration.id.value}",
                    )
                }
                if (contracts.isNotEmpty() && declaredExclusions.isNotEmpty()) {
                    issues += EntrySourceSdkConsumerCoverageIssue(
                        responsibleOwner = input.owner,
                        contextInput = input.id,
                        details = "Source context input ${input.id.value} cannot declare SDK contracts and an " +
                            "exclusion for integration ${integration.id.value}",
                    )
                }
                if (declaredExclusions.size > 1) {
                    issues += EntrySourceSdkConsumerCoverageIssue(
                        responsibleOwner = input.owner,
                        contextInput = input.id,
                        details = "Source context input ${input.id.value} has multiple SDK coverage exclusions for " +
                            "integration ${integration.id.value}",
                    )
                }
                declaredExclusions.singleOrNull()?.let { exclusion ->
                    exclusions += EntrySourceSdkContextExclusionRecord(
                        feature = feature.feature,
                        integration = integration.id,
                        contextInput = input.id,
                        owner = input.owner,
                        reason = exclusion.reason,
                    )
                }
                contracts.forEach { classification ->
                    consumers += EntrySourceSdkContextConsumer(
                        contract = classification.contract,
                        feature = feature.feature,
                        featureOwner = feature.owner,
                        integration = integration.id,
                        contextInput = input.id,
                    )
                }
            }
        }
    }

    val sortedConsumers = consumers.distinct().sortedWith(
        compareBy(
            { it.contract.qualifiedName },
            { it.feature.value },
            { it.integration.value },
            { it.contextInput.value },
        ),
    )

    return EntrySourceSdkConsumerCoveragePlan(
        consumers = sortedConsumers,
        exclusions = exclusions.sortedWith(
            compareBy({
                it.feature.value
            }, { it.integration.value }, { it.contextInput.value }),
        ),
        issues = issues,
    )
}

private fun EntrySourceSdkContextClassification.appliesTo(integration: mihon.feature.graph.FeatureIntegrationId) =
    when (this) {
        is EntrySourceSdkContractContext -> integrations.isEmpty() || integration in integrations
        is EntrySourceSdkContextExclusion -> integrations.isEmpty() || integration in integrations
    }
