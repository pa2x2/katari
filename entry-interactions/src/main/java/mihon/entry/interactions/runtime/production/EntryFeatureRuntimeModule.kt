package mihon.entry.interactions

import android.app.Application
import mihon.feature.graph.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.FeatureExecutionParticipantBinding
import mihon.feature.graph.FeatureGraphContributor
import uy.kohesive.injekt.api.InjektRegistrar
import kotlin.reflect.KClass

/**
 * One production installation unit for a Feature's graph declaration and runtime implementation.
 *
 * The production topology installs this unit once. Graph discovery, execution bindings, and warmups are then derived
 * from the installed result instead of being maintained as independent completion lists.
 */
internal class EntryFeatureRuntimeModule(
    val id: String,
    val contributor: FeatureGraphContributor,
    val additionalContributors: List<FeatureGraphContributor> = emptyList(),
    val installRuntime: InjektRegistrar.(EntryFeatureRuntimeInstallationContext) -> EntryFeatureRuntimeArtifacts,
) {
    init {
        require(id.isNotBlank()) { "Entry Feature runtime module id cannot be blank" }
    }

    val graphContributors: List<FeatureGraphContributor>
        get() = listOf(contributor) + additionalContributors
}

internal data class EntryFeatureRuntimeInstallationContext(
    val application: Application,
    val dependencies: EntryInteractionRuntimeDependencies,
)

internal data class EntryFeatureRuntimeArtifacts(
    val executionBindings: List<FeatureExecutionParticipantBinding<*>> = emptyList(),
    val durableExecutionBindings: List<FeatureDurableExecutionParticipantBinding<*>> = emptyList(),
    val runtimeBoundaries: List<EntryFeatureRuntimeBoundary<*>> = emptyList(),
    val warmups: List<() -> Unit> = emptyList(),
)

internal data class EntryFeatureRuntimeBoundary<T : Any>(
    val type: KClass<T>,
    val resolve: () -> T,
)

internal inline fun <reified T : Any> entryFeatureRuntimeBoundary(
    noinline resolve: () -> T,
): EntryFeatureRuntimeBoundary<T> = EntryFeatureRuntimeBoundary(T::class, resolve)

internal data class InstalledEntryFeatureRuntimeModule(
    val module: EntryFeatureRuntimeModule,
    val artifacts: EntryFeatureRuntimeArtifacts,
)

internal data class EntryFeatureRuntimeInstallation(
    val modules: List<InstalledEntryFeatureRuntimeModule>,
)

internal fun installEntryFeatureRuntimeModules(
    registrar: InjektRegistrar,
    modules: List<EntryFeatureRuntimeModule>,
    context: EntryFeatureRuntimeInstallationContext,
): List<InstalledEntryFeatureRuntimeModule> {
    val duplicateIds = modules.groupBy(EntryFeatureRuntimeModule::id).filterValues { it.size > 1 }
    check(duplicateIds.isEmpty()) {
        "Duplicate Entry Feature runtime modules: ${duplicateIds.keys.sorted()}"
    }
    val duplicateContributors = modules
        .flatMap { module -> module.graphContributors.map { it to module.id } }
        .groupBy({ it.first }, { it.second })
        .filterValues { it.size > 1 }
    check(duplicateContributors.isEmpty()) {
        "Entry Feature graph contributors installed by multiple runtime modules: " +
            duplicateContributors.values.map(List<String>::sorted)
    }
    val installed = modules.map { module ->
        InstalledEntryFeatureRuntimeModule(
            module = module,
            artifacts = module.installRuntime(registrar, context),
        )
    }
    val duplicateRuntimeBoundaries = installed
        .flatMap { installedModule ->
            installedModule.artifacts.runtimeBoundaries.map { boundary -> boundary.type to installedModule.module.id }
        }
        .groupBy({ it.first }, { it.second })
        .filterValues { it.size > 1 }
    check(duplicateRuntimeBoundaries.isEmpty()) {
        "Entry Feature runtime boundaries are installed by multiple modules: $duplicateRuntimeBoundaries"
    }
    return installed
}

internal fun validateInstalledEntryFeatureRuntimeModules(
    installed: List<InstalledEntryFeatureRuntimeModule>,
) {
    installed.forEach { installedModule ->
        installedModule.artifacts.runtimeBoundaries.forEach { boundary ->
            val resolved = boundary.resolve()
            check(boundary.type.isInstance(resolved)) {
                "Entry Feature module ${installedModule.module.id} resolved ${resolved::class} for ${boundary.type}"
            }
        }
    }
}
