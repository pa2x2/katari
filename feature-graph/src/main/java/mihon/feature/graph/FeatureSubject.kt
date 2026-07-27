package mihon.feature.graph

/** The two application boundaries against which Feature applicability can be evaluated. */
sealed interface FeatureSubjectScope {
    data object Application : FeatureSubjectScope

    data object EntryContentType : FeatureSubjectScope
}

/** Stable identity of one installed subject against which Features are evaluated. */
sealed interface FeatureSubjectId {
    val scope: FeatureSubjectScope
    val stableValue: String

    data object Application : FeatureSubjectId {
        override val scope = FeatureSubjectScope.Application
        override val stableValue = "application"
    }

    data class EntryContentType(
        val contentType: ContentTypeId,
    ) : FeatureSubjectId {
        override val scope = FeatureSubjectScope.EntryContentType
        override val stableValue = "entry-content-type:${contentType.value}"
    }
}

/** Installed capability, adapter, and fixture contributions for one Feature subject. */
sealed interface FeatureSubjectContribution {
    val subject: FeatureSubjectId
    val owner: ContributionOwner
    val providers: List<CapabilityProvider<*>>
    val specializedAdapters: List<SpecializedAdapter<*>>
    val contractFixtures: List<ContractFixture<*>>
}

/** The single installed application subject. */
data class ApplicationSubjectContribution(
    override val owner: ContributionOwner,
    override val providers: List<CapabilityProvider<*>> = emptyList(),
    override val specializedAdapters: List<SpecializedAdapter<*>> = emptyList(),
    override val contractFixtures: List<ContractFixture<*>> = emptyList(),
) : FeatureSubjectContribution {
    override val subject = FeatureSubjectId.Application

    init {
        requireUnique(
            label = "Capability providers for application",
            ids = providers.map { it.capability.id.value },
        )
        requireUnique(
            label = "Specialized adapters for application",
            ids = specializedAdapters.map { it.definition.id.value },
        )
        requireUnique(
            label = "Contract fixtures for application",
            ids = contractFixtures.map { it.definition.id.value },
        )
    }
}

/** Stable evaluated reference to an installed Feature subject and its responsible owner. */
data class FeatureSubjectReference(
    val id: FeatureSubjectId,
    val owner: ContributionOwner,
)

internal fun FeatureSubjectContribution.reference(): FeatureSubjectReference =
    FeatureSubjectReference(subject, owner)

internal val FeatureSubjectId.displayValue: String
    get() = when (this) {
        FeatureSubjectId.Application -> stableValue
        is FeatureSubjectId.EntryContentType -> contentType.value
    }
