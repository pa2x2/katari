package mihon.core.migration.migrations

import eu.kanade.domain.source.service.SourcePreferences
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import mihon.domain.extension.model.ContentWarning
import mihon.feature.profiles.core.ProfileConstants
import tachiyomi.core.common.util.lang.withIOContext

/**
 * "Not rated" is allowed by default, but a profile that chose its content warnings before it existed only has the
 * ones it chose. One that allows 18+ gets it too, since nothing unrated can be more than that; one restricted below
 * 18+, such as a children's profile, stays without it.
 */
class NotRatedContentWarningMigration : Migration {
    override val version: Float = 86f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val profileIds = migrationContext.profileDatabase.getProfiles(includeArchived = true).map { it.id }
            .ifEmpty { listOf(ProfileConstants.DEFAULT_PROFILE_ID) }

        profileIds.forEach { profileId ->
            val enabled = SourcePreferences.enabledContentWarnings(
                migrationContext.profileStore.profileStore(profileId),
            )
            if (enabled.isSet() && ContentWarning.NSFW in enabled.get()) {
                enabled.set(enabled.get() + ContentWarning.NOT_RATED)
            }
        }

        return@withIOContext true
    }
}
