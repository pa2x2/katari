package app.katari.upgrade.migrations

import app.katari.profiles.core.ProfileConstants
import app.katari.upgrade.Migration
import app.katari.upgrade.MigrationContext
import eu.kanade.domain.source.service.LegacySourceDisplayModesConversion
import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.serialization.json.Json
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.library.service.LibraryPreferences

/**
 * Gives Browse a layout of its own: the mode most sources shared, and the cover size catalogs showed until now, the
 * library's. A cover size stored as `Auto` is stored again as the Medium it now reads as.
 */
class CoverLayoutMigration : Migration {
    override val version: Float = 93f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val profileStore = migrationContext.profileStore
        val profileDatabase = migrationContext.profileDatabase
        val profileIds = profileDatabase.getProfiles(includeArchived = true).map { it.id }
            .ifEmpty { listOf(ProfileConstants.DEFAULT_PROFILE_ID) }

        profileIds.forEach { profileId ->
            val store = profileStore.profileStore(profileId)
            LegacySourceDisplayModesConversion.convert(store)
            seedBrowseCoverSize(store)
        }

        return@withIOContext true
    }

    private fun seedBrowseCoverSize(store: PreferenceStore) {
        val librarySize = LibraryPreferences(store).coverSize
        if (!librarySize.isSet()) return
        val size = librarySize.get()
        librarySize.set(size)
        SourcePreferences(store, Json).browseCoverSize.set(size)
    }
}
