package app.katari.desktop.profile

import app.katari.profiles.core.LegacyProfilePreferenceKeys
import dev.zacsweers.metro.Inject

/** Desktop has kept its preferences in profile files from the start, so no older keys are left to move. */
@Inject
class NoLegacyProfilePreferenceKeys : LegacyProfilePreferenceKeys {
    override suspend fun migrate(fromVersion: Int, migrateProfileStores: suspend () -> Unit) = migrateProfileStores()
}
