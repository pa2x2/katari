package mihon.feature.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.SingleIn
import mihon.feature.graph.ApplicationSubjectContribution
import mihon.feature.graph.CapabilityProvider
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.execution.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.graph.featureGraphContributor
import mihon.feature.runtime.injekt.InjektExport
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner

/**
 * What Feature modules contribute to the Feature runtime through the dependency graph: their Feature declarations,
 * the capabilities application Features provide on the application subject, the participants that run their
 * executions, checks of the evaluated graph that only the owning Feature can make, and work to start once the
 * runtime is assembled.
 */
@Inject
@SingleIn(AppScope::class)
class FeatureRuntimeContributions(
    graphContributors: Set<FeatureGraphContributor>,
    capabilityProviders: Set<CapabilityProvider<*>>,
    executionBindings: Set<FeatureExecutionParticipantBinding<*>>,
    durableExecutionBindings: Set<FeatureDurableExecutionParticipantBinding<*>>,
    private val graphValidators: Set<FeatureGraphValidator>,
    private val warmups: Set<FeatureRuntimeWarmup>,
) {
    val featureRuntimeInputs: FeatureRuntimeInputs = FeatureRuntimeInputs(
        graphContributors = graphContributors.toList() + featureGraphContributor(APPLICATION_FEATURE_SUBJECT_OWNER) {
            add(
                ApplicationSubjectContribution(
                    owner = APPLICATION_FEATURE_SUBJECT_OWNER,
                    providers = capabilityProviders.toList(),
                ),
            )
        },
        executionBindings = executionBindings.toList(),
        durableExecutionBindings = durableExecutionBindings.toList(),
    )

    fun validate(evaluation: FeatureGraphEvaluation) {
        graphValidators.forEach { it.validate(evaluation) }
    }

    fun warmUp() {
        warmups.forEach(FeatureRuntimeWarmup::warmup)
    }
}

fun interface FeatureGraphValidator {
    fun validate(evaluation: FeatureGraphEvaluation)
}

@ContributesTo(AppScope::class)
interface FeatureRuntimeMultibindings {
    @Multibinds(allowEmpty = true)
    fun graphContributors(): Set<FeatureGraphContributor>

    @Multibinds(allowEmpty = true)
    fun capabilityProviders(): Set<CapabilityProvider<*>>

    @Multibinds(allowEmpty = true)
    fun executionBindings(): Set<FeatureExecutionParticipantBinding<*>>

    @Multibinds(allowEmpty = true)
    fun durableExecutionBindings(): Set<FeatureDurableExecutionParticipantBinding<*>>

    @Multibinds(allowEmpty = true)
    fun graphValidators(): Set<FeatureGraphValidator>

    @Multibinds(allowEmpty = true)
    fun warmups(): Set<FeatureRuntimeWarmup>

    @Multibinds(allowEmpty = true)
    fun profilePreferenceOwners(): Set<InstalledProfilePreferenceOwner<*>>

    @Multibinds(allowEmpty = true)
    fun injektExports(): Set<InjektExport<*>>
}

private val APPLICATION_FEATURE_SUBJECT_OWNER = ContributionOwner("application-feature-runtime")
