package mihon.core.migration.migrations

import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import mihon.feature.library.update.legacy.LegacyUpdateCategoriesConversion
import mihon.feature.profiles.core.ProfileConstants
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository

/**
 * Moves library update filtering to update rules without changing what existing profiles check.
 *
 * New installs skip fewer entries by default, so a profile that never changed its skip rules gets the old default
 * written explicitly. Category include and exclude lists become category rules. Updates no longer have a switch for
 * hiding chapters from excluded scanlators because they are always hidden.
 */
class LibraryUpdateRulesMigration : Migration {
    override val version: Float = 69f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val profileStore = migrationContext.get<ProfileStore>() ?: return@withIOContext false
        val profileDatabase = migrationContext.get<ProfileDatabase>() ?: return@withIOContext false
        val categoryRepository = migrationContext.get<CategoryRepository>() ?: return@withIOContext false
        val rulesRepository = migrationContext.get<LibraryUpdateRulesRepository>() ?: return@withIOContext false
        val categoriesConversion = LegacyUpdateCategoriesConversion(categoryRepository, rulesRepository)
        val profileIds = profileDatabase.getProfiles(includeArchived = true).map { it.id }
            .ifEmpty { listOf(ProfileConstants.DEFAULT_PROFILE_ID) }

        profileIds.forEach { profileId ->
            val store = profileStore.profileStore(profileId)
            val skipRules = store.getStringSet(LibraryPreferences.UPDATE_SKIP_RULES_PREF_KEY)
            if (!skipRules.isSet()) {
                skipRules.set(LEGACY_DEFAULT_SKIP_RULES)
            }
            categoriesConversion.convert(profileId, store)
            store.getBoolean(LEGACY_HIDE_EXCLUDED_SCANLATORS).delete()
        }

        return@withIOContext true
    }

    private companion object {
        val LEGACY_DEFAULT_SKIP_RULES = setOf(
            LibraryPreferences.SKIP_COMPLETED,
            LibraryPreferences.SKIP_UNSEEN,
            LibraryPreferences.SKIP_NOT_STARTED,
            LibraryPreferences.SKIP_OUTSIDE_RELEASE_PERIOD,
        )
        const val LEGACY_HIDE_EXCLUDED_SCANLATORS = "pref_filter_updates_hide_excluded_scanlators"
    }
}
