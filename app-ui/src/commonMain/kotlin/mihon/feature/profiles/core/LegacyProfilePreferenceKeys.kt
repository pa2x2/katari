package mihon.feature.profiles.core

/**
 * Moves preferences that older app versions kept under other keys to the keys profiles read now. Android kept every
 * preference in one shared-preferences file, first without profiles, so it has keys to move; desktop never did.
 */
interface LegacyProfilePreferenceKeys {
    /**
     * Moves what versions before migration version [fromVersion] left behind, then runs [migrateProfileStores], which
     * reads the moved values through the profile stores, and only after it removes the keys they were moved from.
     */
    suspend fun migrate(fromVersion: Int, migrateProfileStores: suspend () -> Unit)
}
