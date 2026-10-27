package mihon.desktop.profile

import dev.zacsweers.metro.Inject
import mihon.feature.profiles.core.LegacyProfilePreferenceKeys

/** Desktop has kept its preferences in profile files from the start, so no older keys are left to move. */
@Inject
class NoLegacyProfilePreferenceKeys : LegacyProfilePreferenceKeys {
    override suspend fun migrate(fromVersion: Int, migrateProfileStores: suspend () -> Unit) = migrateProfileStores()
}
