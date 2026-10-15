package mihon.feature.runtime

import mihon.feature.runtime.injekt.InjektExport
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import uy.kohesive.injekt.api.InjektRegistrar

/** A dependency graph holding the Feature modules, as the app and the production validation read it. */
interface FeatureRuntimeGraph {
    val featureRuntimeContributions: FeatureRuntimeContributions

    val profilePreferenceOwners: Set<InstalledProfilePreferenceOwner<*>>

    val injektExports: Set<InjektExport<*>>
}

/** Registers the graph's profile preference owners and serves its bindings to code that resolves them through Injekt. */
fun InjektRegistrar.addFeatureRuntimeGraph(graph: FeatureRuntimeGraph) {
    // Profile copy and backup derive preference ownership from every registered owner, so owners register at
    // startup instead of when their Feature is first used.
    graph.profilePreferenceOwners
    graph.injektExports.forEach { it.registerIn(this) }
}
