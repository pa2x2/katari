package mihon.feature.graph

/**
 * Deterministic structural graph of discovered contributions.
 *
 * This graph contains relationships but does not evaluate applicability or materialize obligations.
 */
data class FeatureGraph(
    val contentTypes: List<ContentTypeContribution>,
    val features: List<FeatureContribution>,
    val executionPoints: List<FeatureExecutionPointDefinition<*>>,
    val executionParticipants: List<FeatureExecutionParticipantDefinition<*>>,
    val capabilities: List<CapabilityDefinition<*>>,
    val contextInputs: List<ContextInputDefinition<*>>,
    val specializedAdapters: List<SpecializedAdapterDefinition<*>>,
    val contractFixtures: List<ContractFixtureDefinition<*>>,
    val projections: List<FeatureProjectionDefinition<*>>,
)

fun assembleFeatureGraph(
    discovered: DiscoveredFeatureGraphContributions,
): FeatureGraph {
    validateUniqueContentTypes(discovered.contentTypes)
    validateUniqueFeatures(discovered.features)
    validateUniqueExecutionPoints(discovered.executionPoints)

    val capabilityDefinitions = buildList {
        discovered.contentTypes.flatMapTo(this) { type -> type.providers.map { it.capability } }
        discovered.features.flatMapTo(this) { feature ->
            feature.integrations.flatMap { integration -> integration.prerequisites.capabilities() }
        }
        discovered.executionParticipants.flatMapTo(this) { participant ->
            participant.prerequisites.capabilities()
        }
    }
    val contextInputDefinitions = buildList {
        discovered.features.flatMapTo(this) { feature ->
            feature.integrations.flatMap { it.contextInputs }
        }
        discovered.executionParticipants.flatMapTo(this) { it.contextInputs }
    }
    val specializedAdapterDefinitions = buildList {
        discovered.contentTypes.flatMapTo(this) { type ->
            type.specializedAdapters.map { it.definition }
        }
        discovered.features.flatMapTo(this) { feature ->
            feature.integrations.flatMap { it.specializedPrerequisites + it.specializedRequirements }
        }
        discovered.executionParticipants.flatMapTo(this) { participant ->
            participant.specializedPrerequisites + participant.specializedRequirements
        }
    }
    val contractFixtureDefinitions = buildList {
        discovered.contentTypes.flatMapTo(this) { type ->
            type.contractFixtures.map { it.definition }
        }
        discovered.features.flatMapTo(this) { feature ->
            feature.integrations
                .flatMap { it.behavioralContracts }
                .flatMap { it.fixtureRequirements }
        }
        discovered.executionParticipants.flatMapTo(this) { participant ->
            participant.behavioralContracts.flatMap { it.fixtureRequirements }
        }
    }
    val projectionDefinitions = buildList {
        discovered.features.flatMapTo(this) { feature ->
            feature.integrations.flatMap { integration ->
                integration.projectionRequirements + integration.projections.map { it.definition }
            }
        }
    }

    val capabilities = consistentDefinitions(
        label = "capability",
        definitions = capabilityDefinitions,
        id = { it.id.value },
    )
    val contextInputs = consistentDefinitions(
        label = "context input",
        definitions = contextInputDefinitions,
        id = { it.id.value },
    )
    val specializedAdapters = consistentDefinitions(
        label = "specialized adapter",
        definitions = specializedAdapterDefinitions,
        id = { it.id.value },
    )
    val contractFixtures = consistentDefinitions(
        label = "contract fixture",
        definitions = contractFixtureDefinitions,
        id = { it.id.value },
    )
    val projections = consistentDefinitions(
        label = "projection",
        definitions = projectionDefinitions,
        id = { "${it.owner.value}:${it.id.value}" },
    )
    val executionPoints = consistentDefinitions(
        label = "execution point",
        definitions = discovered.executionPoints + discovered.executionParticipants.map { it.point },
        id = { it.id.value },
    )
    validateUniqueExecutionParticipants(discovered.executionParticipants)
    validateExecutionParticipation(
        points = executionPoints,
        declaredPoints = discovered.executionPoints,
        participants = discovered.executionParticipants,
    )

    validateReachability(discovered)

    return FeatureGraph(
        contentTypes = discovered.contentTypes.sortedBy { it.contentType.value },
        features = discovered.features.sortedBy { it.feature.value },
        executionPoints = executionPoints.sortedBy { it.id.value },
        executionParticipants = discovered.executionParticipants.sortedBy { it.id.value },
        capabilities = capabilities.sortedBy { it.id.value },
        contextInputs = contextInputs.sortedBy { it.id.value },
        specializedAdapters = specializedAdapters.sortedBy { it.id.value },
        contractFixtures = contractFixtures.sortedBy { it.id.value },
        projections = projections.sortedWith(compareBy({ it.owner.value }, { it.id.value })),
    )
}

private fun validateUniqueExecutionPoints(points: List<FeatureExecutionPointDefinition<*>>) {
    points.groupBy { it.id }
        .filterValues { it.size > 1 }
        .forEach { (id, duplicates) ->
            val owners = duplicates.map { it.owner }.distinct().sortedBy { it.value }
            error("Duplicate execution point $id from owners $owners")
        }
}

private fun validateUniqueExecutionParticipants(
    participants: List<FeatureExecutionParticipantDefinition<*>>,
) {
    participants.groupBy { it.id }
        .filterValues { it.size > 1 }
        .forEach { (id, duplicates) ->
            val owners = duplicates.map { it.owner }.distinct().sortedBy { it.value }
            error("Duplicate execution participant $id from owners $owners")
        }
}

private fun validateExecutionParticipation(
    points: List<FeatureExecutionPointDefinition<*>>,
    declaredPoints: List<FeatureExecutionPointDefinition<*>>,
    participants: List<FeatureExecutionParticipantDefinition<*>>,
) {
    val pointsById = points.associateBy { it.id }
    val declaredPointIds = declaredPoints.mapTo(mutableSetOf()) { it.id }
    participants.forEach { participant ->
        check(participant.point.id in declaredPointIds) {
            "Execution participant ${participant.id} targets undeclared point ${participant.point.id}"
        }
        val point = requireNotNull(pointsById[participant.point.id]) {
            "Execution participant ${participant.id} targets unknown point ${participant.point.id}"
        }
        check(point == participant.point) {
            "Execution participant ${participant.id} targets contradictory point definition ${participant.point.id}"
        }
    }
    points.forEach { point ->
        check(participants.any { it.point.id == point.id }) {
            "Unreachable execution point ${point.id}: no participant targets it"
        }
        orderedExecutionParticipants(point, participants)
    }
}

internal fun orderedExecutionParticipants(
    point: FeatureExecutionPointDefinition<*>,
    participants: List<FeatureExecutionParticipantDefinition<*>>,
): List<FeatureExecutionParticipantDefinition<*>> {
    val matching = participants.filter { it.point.id == point.id }
    val byId = matching.associateBy { it.id }
    val edges = matching.associate { it.id to mutableSetOf<FeatureExecutionParticipantId>() }
    matching.forEach { participant ->
        participant.order.after.forEach { predecessor ->
            val referenced = requireNotNull(byId[predecessor]) {
                "Execution participant ${participant.id} orders itself after unknown participant $predecessor"
            }
            check(referenced.point.id == point.id)
            edges.getValue(predecessor) += participant.id
        }
        participant.order.before.forEach { successor ->
            val referenced = requireNotNull(byId[successor]) {
                "Execution participant ${participant.id} orders itself before unknown participant $successor"
            }
            check(referenced.point.id == point.id)
            edges.getValue(participant.id) += successor
        }
    }

    val incoming = matching.associate { participant ->
        participant.id to edges.values.count { participant.id in it }
    }.toMutableMap()
    val ready = java.util.PriorityQueue(compareBy<FeatureExecutionParticipantId> { it.value })
    incoming.filterValues { it == 0 }.keys.forEach(ready::add)
    val ordered = mutableListOf<FeatureExecutionParticipantDefinition<*>>()
    while (ready.isNotEmpty()) {
        val id = ready.remove()
        ordered += byId.getValue(id)
        edges.getValue(id).sortedBy { it.value }.forEach { successor ->
            val remaining = incoming.getValue(successor) - 1
            incoming[successor] = remaining
            if (remaining == 0) ready += successor
        }
    }
    check(ordered.size == matching.size) {
        val cycle = incoming.filterValues { it > 0 }.keys.sortedBy { it.value }
        "Execution point ${point.id} contains cyclic participant ordering: $cycle"
    }
    return ordered
}

fun discoverAndAssembleFeatureGraph(
    contributors: Iterable<FeatureGraphContributor>,
): FeatureGraph = assembleFeatureGraph(discoverFeatureGraphContributions(contributors))

private fun validateUniqueContentTypes(contributions: List<ContentTypeContribution>) {
    contributions.groupBy { it.contentType }
        .filterValues { it.size > 1 }
        .forEach { (id, duplicates) ->
            val owners = duplicates.map { it.owner }.distinct().sortedBy { it.value }
            error("Duplicate content-type contribution $id from owners $owners")
        }
}

private fun validateUniqueFeatures(contributions: List<FeatureContribution>) {
    contributions.groupBy { it.feature }
        .filterValues { it.size > 1 }
        .forEach { (id, duplicates) ->
            val owners = duplicates.map { it.owner }.distinct().sortedBy { it.value }
            error("Duplicate feature contribution $id from owners $owners")
        }
}

private fun <D> consistentDefinitions(
    label: String,
    definitions: List<D>,
    id: (D) -> String,
): List<D> {
    return definitions.groupBy(id)
        .map { (definitionId, matchingDefinitions) ->
            check(matchingDefinitions.distinct().size == 1) {
                "Contradictory $label definition $definitionId: ${matchingDefinitions.distinct()}"
            }
            matchingDefinitions.first()
        }
}

private fun validateReachability(discovered: DiscoveredFeatureGraphContributions) {
    val consumedCapabilities = discovered.features
        .flatMap { it.integrations }
        .flatMap { it.prerequisites.capabilities() }
        .mapTo(mutableSetOf()) { it.id }
        .apply {
            discovered.executionParticipants
                .flatMap { it.prerequisites.capabilities() }
                .mapTo(this) { it.id }
        }
    val requiredAdapters = discovered.features
        .flatMap { it.integrations }
        .flatMap { it.specializedPrerequisites + it.specializedRequirements }
        .mapTo(mutableSetOf()) { it.id }
        .apply {
            discovered.executionParticipants
                .flatMap { it.specializedPrerequisites + it.specializedRequirements }
                .mapTo(this) { it.id }
        }
    val requiredFixtures = discovered.features
        .flatMap { it.integrations }
        .flatMap { it.behavioralContracts }
        .flatMap { it.fixtureRequirements }
        .mapTo(mutableSetOf()) { it.id }
        .apply {
            discovered.executionParticipants
                .flatMap { it.behavioralContracts }
                .flatMap { it.fixtureRequirements }
                .mapTo(this) { it.id }
        }

    discovered.contentTypes.forEach { type ->
        type.providers.forEach { provider ->
            check(provider.capability.id in consumedCapabilities) {
                "Unreachable capability provider ${provider.capability.id} on ${type.contentType}: " +
                    "no feature integration consumes it"
            }
        }
        type.specializedAdapters.forEach { adapter ->
            check(adapter.definition.id in requiredAdapters) {
                "Unreachable specialized adapter ${adapter.definition.id} on ${type.contentType}: " +
                    "no feature integration requires it"
            }
        }
        type.contractFixtures.forEach { fixture ->
            check(fixture.definition.id in requiredFixtures) {
                "Unreachable contract fixture ${fixture.definition.id} on ${type.contentType}: " +
                    "no behavioral contract requires it"
            }
        }
    }

    discovered.features.forEach { feature ->
        feature.integrations.forEach { integration ->
            val hasEffect = integration.specializedPrerequisites.isNotEmpty() ||
                integration.specializedRequirements.isNotEmpty() ||
                integration.behaviorProjections.isNotEmpty() ||
                integration.behavioralContracts.isNotEmpty() ||
                integration.projectionRequirements.isNotEmpty()
            check(hasEffect) {
                "Unreachable feature integration ${integration.id} on ${feature.feature}: " +
                    "it contributes no behavior projection, specialized requirement, contract, or projection"
            }
        }
    }
}

private fun CapabilityExpression.capabilities(): List<CapabilityDefinition<*>> {
    return when (this) {
        CapabilityExpression.Always -> emptyList()
        is CapabilityExpression.Provided -> listOf(capability)
        is CapabilityExpression.AllOf -> terms.flatMap { it.capabilities() }
        is CapabilityExpression.AnyOf -> terms.flatMap { it.capabilities() }
    }
}
