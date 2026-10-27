package mihon.desktop.source

import dev.zacsweers.metro.Inject
import mihon.feature.profiles.core.ProfileExtensions

/**
 * Desktop loads no extensions yet, so a new profile has no installed sources to hide and a deleted one no extension
 * settings to remove. Stands in until desktop hosts extensions.
 */
@Inject
class NoProfileExtensions : ProfileExtensions {
    override suspend fun installedSourceIds(): Set<Long> = emptySet()

    override fun deleteSettings(profileId: Long) = Unit
}
