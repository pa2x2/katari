package mihon.feature.graph

import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition
import mihon.feature.graph.execution.FeatureExecutionPointDefinition

/**
 * Owning composition boundary for one or more subject or feature contributions.
 *
 * The environment supplies contributors to discovery. The graph kernel never maintains a list of concrete types,
 * capabilities, or features.
 */
interface FeatureGraphContributor {
    val owner: ContributionOwner

    fun contributeTo(sink: FeatureGraphContributionSink)
}

fun featureGraphContributor(
    owner: ContributionOwner,
    contribute: FeatureGraphContributionSink.() -> Unit,
): FeatureGraphContributor {
    return object : FeatureGraphContributor {
        override val owner = owner

        override fun contributeTo(sink: FeatureGraphContributionSink) {
            sink.contribute()
        }
    }
}

/** The only surface through which independently owned contributions enter discovery. */
class FeatureGraphContributionSink internal constructor(
    private val owner: ContributionOwner,
) {
    private val contentTypes = mutableListOf<ContentTypeContribution>()
    private val applicationSubjects = mutableListOf<ApplicationSubjectContribution>()
    private val features = mutableListOf<FeatureContribution>()
    private val executionPoints = mutableListOf<FeatureExecutionPointDefinition<*>>()
    private val executionParticipants = mutableListOf<FeatureExecutionParticipantDefinition<*>>()

    fun add(contribution: ContentTypeContribution) {
        require(contribution.owner == owner) {
            "Contributor $owner cannot submit content type ${contribution.contentType} owned by ${contribution.owner}"
        }
        contentTypes += contribution
    }

    fun add(contribution: ApplicationSubjectContribution) {
        require(contribution.owner == owner) {
            "Contributor $owner cannot submit application subject owned by ${contribution.owner}"
        }
        applicationSubjects += contribution
    }

    fun add(contribution: FeatureContribution) {
        require(contribution.owner == owner) {
            "Contributor $owner cannot submit feature ${contribution.feature} owned by ${contribution.owner}"
        }
        features += contribution
    }

    fun add(definition: FeatureExecutionPointDefinition<*>) {
        require(definition.owner == owner) {
            "Contributor $owner cannot submit execution point ${definition.id} owned by ${definition.owner}"
        }
        executionPoints += definition
    }

    fun add(definition: FeatureExecutionParticipantDefinition<*>) {
        require(definition.owner == owner) {
            "Contributor $owner cannot submit execution participant ${definition.id} owned by ${definition.owner}"
        }
        executionParticipants += definition
    }

    internal fun snapshot(): DiscoveredFeatureGraphContributions {
        return DiscoveredFeatureGraphContributions(
            contentTypes = contentTypes.toList(),
            applicationSubjects = applicationSubjects.toList(),
            features = features.toList(),
            executionPoints = executionPoints.toList(),
            executionParticipants = executionParticipants.toList(),
        )
    }
}

/** Raw discovered contributions before cross-owner graph validation. */
data class DiscoveredFeatureGraphContributions(
    val contentTypes: List<ContentTypeContribution>,
    val features: List<FeatureContribution>,
    val applicationSubjects: List<ApplicationSubjectContribution> = emptyList(),
    val executionPoints: List<FeatureExecutionPointDefinition<*>> = emptyList(),
    val executionParticipants: List<FeatureExecutionParticipantDefinition<*>> = emptyList(),
) {
    val subjects: List<FeatureSubjectContribution>
        get() = applicationSubjects + contentTypes
}

fun discoverFeatureGraphContributions(
    contributors: Iterable<FeatureGraphContributor>,
): DiscoveredFeatureGraphContributions {
    val snapshots = contributors.map { contributor ->
        FeatureGraphContributionSink(contributor.owner)
            .also(contributor::contributeTo)
            .snapshot()
    }
    return DiscoveredFeatureGraphContributions(
        contentTypes = snapshots.flatMap { it.contentTypes },
        applicationSubjects = snapshots.flatMap { it.applicationSubjects },
        features = snapshots.flatMap { it.features },
        executionPoints = snapshots.flatMap { it.executionPoints },
        executionParticipants = snapshots.flatMap { it.executionParticipants },
    )
}
