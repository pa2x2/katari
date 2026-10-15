package mihon.feature.runtime

import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner

/** A dependency graph holding the Feature modules, as the app and the production validation read it. */
interface FeatureRuntimeGraph {
    val featureRuntimeContributions: FeatureRuntimeContributions

    val profilePreferenceOwners: Set<InstalledProfilePreferenceOwner<*>>
}

/**
 * Profile copy and backup derive preference ownership from every registered owner, so owners register at startup
 * instead of when their Feature is first used. Building the set is what registers them.
 */
fun FeatureRuntimeGraph.registerProfilePreferenceOwners() {
    profilePreferenceOwners
}
