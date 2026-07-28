package mihon.feature.runtime

import android.app.Application
import mihon.feature.graph.ApplicationSubjectContribution
import mihon.feature.graph.CapabilityProvider
import mihon.feature.graph.ContractFixture
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.FeatureExecutionParticipantBinding
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.SpecializedAdapter
import mihon.feature.graph.featureGraphContributor
import uy.kohesive.injekt.api.InjektRegistrar
import kotlin.reflect.KClass

/**
 * One owner-local installation unit for an application-scoped Feature.
 *
 * The module keeps its Feature declaration and runtime implementation together. The application subject itself is
 * aggregated once from every installed module.
 */
class ApplicationFeatureRuntimeModule(
    val id: String,
    val contributor: FeatureGraphContributor,
    val additionalContributors: List<FeatureGraphContributor> = emptyList(),
    val installRuntime:
    InjektRegistrar.(ApplicationFeatureRuntimeInstallationContext) -> ApplicationFeatureRuntimeArtifacts,
) {
    init {
        require(APPLICATION_FEATURE_MODULE_ID.matches(id)) {
            "Application Feature runtime module id '$id' is invalid"
        }
    }

    val graphContributors: List<FeatureGraphContributor>
        get() = listOf(contributor) + additionalContributors
}

data class ApplicationFeatureRuntimeInstallationContext(
    val application: Application,
)

data class ApplicationFeatureRuntimeArtifacts(
    val capabilityProviders: List<CapabilityProvider<*>> = emptyList(),
    val specializedAdapters: List<SpecializedAdapter<*>> = emptyList(),
    val contractFixtures: List<ContractFixture<*>> = emptyList(),
    val executionBindings: List<FeatureExecutionParticipantBinding<*>> = emptyList(),
    val durableExecutionBindings: List<FeatureDurableExecutionParticipantBinding<*>> = emptyList(),
    val runtimeBoundaries: List<ApplicationFeatureRuntimeBoundary<*>> = emptyList(),
    val graphValidators: List<ApplicationFeatureRuntimeGraphValidator> = emptyList(),
    val warmups: List<() -> Unit> = emptyList(),
)

data class ApplicationFeatureRuntimeBoundary<T : Any>(
    val type: KClass<T>,
    val resolve: () -> T,
)

inline fun <reified T : Any> applicationFeatureRuntimeBoundary(
    noinline resolve: () -> T,
): ApplicationFeatureRuntimeBoundary<T> = ApplicationFeatureRuntimeBoundary(T::class, resolve)

fun interface ApplicationFeatureRuntimeGraphValidator {
    fun validate(evaluation: FeatureGraphEvaluation)
}

data class InstalledApplicationFeatureRuntimeModule(
    val module: ApplicationFeatureRuntimeModule,
    val artifacts: ApplicationFeatureRuntimeArtifacts,
)

data class ApplicationFeatureRuntimeInstallation(
    val modules: List<InstalledApplicationFeatureRuntimeModule>,
    val featureRuntimeInputs: FeatureRuntimeInputs,
    val warmups: List<() -> Unit>,
)

fun installApplicationFeatureRuntimeModules(
    registrar: InjektRegistrar,
    modules: List<ApplicationFeatureRuntimeModule>,
    context: ApplicationFeatureRuntimeInstallationContext,
): ApplicationFeatureRuntimeInstallation {
    validateApplicationFeatureRuntimeModules(modules)
    val installed = modules.map { module ->
        InstalledApplicationFeatureRuntimeModule(
            module = module,
            artifacts = module.installRuntime(registrar, context),
        )
    }
    validateApplicationFeatureRuntimeBoundaries(installed)

    val providers = installed.flatMap { it.artifacts.capabilityProviders }
    val adapters = installed.flatMap { it.artifacts.specializedAdapters }
    val fixtures = installed.flatMap { it.artifacts.contractFixtures }
    val applicationSubject = ApplicationSubjectContribution(
        owner = APPLICATION_FEATURE_SUBJECT_OWNER,
        providers = providers,
        specializedAdapters = adapters,
        contractFixtures = fixtures,
    )
    val subjectContributor = featureGraphContributor(APPLICATION_FEATURE_SUBJECT_OWNER) {
        add(applicationSubject)
    }
    return ApplicationFeatureRuntimeInstallation(
        modules = installed,
        featureRuntimeInputs = FeatureRuntimeInputs(
            graphContributors = listOf(subjectContributor) +
                installed.flatMap { it.module.graphContributors },
            executionBindings = installed.flatMap { it.artifacts.executionBindings },
            durableExecutionBindings = installed.flatMap { it.artifacts.durableExecutionBindings },
        ),
        warmups = installed.flatMap { it.artifacts.warmups },
    )
}

fun validateInstalledApplicationFeatureRuntimeModules(
    installation: ApplicationFeatureRuntimeInstallation,
) {
    installation.modules.forEach { installed ->
        installed.artifacts.runtimeBoundaries.forEach { boundary ->
            val resolved = boundary.resolve()
            check(boundary.type.isInstance(resolved)) {
                "Application Feature module ${installed.module.id} resolved ${resolved::class} for ${boundary.type}"
            }
        }
    }
}

fun validateInstalledApplicationFeatureRuntimeGraph(
    installation: ApplicationFeatureRuntimeInstallation,
    evaluation: FeatureGraphEvaluation,
) {
    installation.modules.forEach { installed ->
        installed.artifacts.graphValidators.forEach { validator ->
            validator.validate(evaluation)
        }
    }
}

private fun validateApplicationFeatureRuntimeModules(
    modules: List<ApplicationFeatureRuntimeModule>,
) {
    val duplicateIds = modules.groupBy(ApplicationFeatureRuntimeModule::id).filterValues { it.size > 1 }
    check(duplicateIds.isEmpty()) {
        "Duplicate Application Feature runtime modules: ${duplicateIds.keys.sorted()}"
    }
    val duplicateContributors = modules
        .flatMap { module -> module.graphContributors.map { it to module.id } }
        .groupBy({ it.first }, { it.second })
        .filterValues { it.size > 1 }
    check(duplicateContributors.isEmpty()) {
        "Application Feature graph contributors installed by multiple runtime modules: " +
            duplicateContributors.values.map(List<String>::sorted)
    }
}

private fun validateApplicationFeatureRuntimeBoundaries(
    installed: List<InstalledApplicationFeatureRuntimeModule>,
) {
    val duplicates = installed
        .flatMap { module ->
            module.artifacts.runtimeBoundaries.map { boundary -> boundary.type to module.module.id }
        }
        .groupBy({ it.first }, { it.second })
        .filterValues { it.size > 1 }
    check(duplicates.isEmpty()) {
        "Application Feature runtime boundaries are installed by multiple modules: $duplicates"
    }
}

private val APPLICATION_FEATURE_SUBJECT_OWNER = ContributionOwner("application-feature-runtime")
private val APPLICATION_FEATURE_MODULE_ID = Regex("""[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*""")
