package mihon.feature.runtime.application

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
import mihon.feature.graph.featureGraphContributor
import mihon.feature.runtime.FeatureRuntimeInputs

/**
 * The application-scoped Features' share of the Feature runtime, aggregated from what each Feature module contributes
 * to the graph: its Feature declarations, the capabilities it provides on the application subject, and checks of the
 * evaluated graph that only the owning Feature can make.
 */
@Inject
@SingleIn(AppScope::class)
class ApplicationFeatureRuntime(
    graphContributors: Set<FeatureGraphContributor>,
    capabilityProviders: Set<CapabilityProvider<*>>,
    private val graphValidators: Set<ApplicationFeatureGraphValidator>,
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
    )

    fun validate(evaluation: FeatureGraphEvaluation) {
        graphValidators.forEach { it.validate(evaluation) }
    }
}

fun interface ApplicationFeatureGraphValidator {
    fun validate(evaluation: FeatureGraphEvaluation)
}

@ContributesTo(AppScope::class)
interface ApplicationFeatureRuntimeMultibindings {
    @Multibinds(allowEmpty = true)
    fun graphContributors(): Set<FeatureGraphContributor>

    @Multibinds(allowEmpty = true)
    fun capabilityProviders(): Set<CapabilityProvider<*>>

    @Multibinds(allowEmpty = true)
    fun graphValidators(): Set<ApplicationFeatureGraphValidator>
}

private val APPLICATION_FEATURE_SUBJECT_OWNER = ContributionOwner("application-feature-runtime")
