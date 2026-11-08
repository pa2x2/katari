package mihon.feature.profiles.core

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry

/**
 * The registry every profile-owned preference class registers its owner with, and the installer that registers them on
 * the active profile's store. Profile copy and backups derive which keys follow a profile from the registry, so every
 * owner has to be registered through it, on every platform.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object ProfilePreferenceOwnerBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun ownerRegistry(): ProfilePreferenceOwnerRegistry = ProfilePreferenceOwnerRegistry()

    @Provides
    @SingleIn(AppScope::class)
    fun ownerInstaller(
        registry: ProfilePreferenceOwnerRegistry,
        profileStore: ProfileStore,
    ): ProfilePreferenceOwnerInstaller = ProfilePreferenceOwnerInstaller(registry) { profileStore.profileStore() }
}
