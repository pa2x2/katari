package mihon.feature.profiles.core

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.category.model.Category

class ProfileDatabase(
    private val handler: DatabaseHandler,
) {
    suspend fun getProfiles(includeArchived: Boolean = true): List<Profile> {
        return handler.awaitList {
            profilesQueries.getProfiles(includeArchived, ::mapProfile)
        }
    }

    fun subscribeProfiles(includeArchived: Boolean = true): Flow<List<Profile>> {
        return handler.subscribeToList {
            profilesQueries.getProfiles(includeArchived, ::mapProfile)
        }
    }

    suspend fun getProfileById(id: Long): Profile? {
        return handler.awaitOneOrNull {
            profilesQueries.getProfileById(id, ::mapProfile)
        }
    }

    suspend fun getProfileByUuid(uuid: String): Profile? {
        return handler.awaitOneOrNull {
            profilesQueries.getProfileByUuid(uuid, ::mapProfile)
        }
    }

    suspend fun insertProfile(
        uuid: String,
        name: String,
        colorSeed: Long,
        position: Long,
        requiresAuth: Boolean,
        isArchived: Boolean,
    ): Long {
        return handler.awaitOneExecutable(inTransaction = true) {
            profilesQueries.insert(
                uuid = uuid,
                name = name,
                colorSeed = colorSeed,
                position = position,
                requiresAuth = requiresAuth,
                isArchived = isArchived,
            )
            profilesQueries.selectLastInsertedRowId()
        }
    }

    suspend fun updateProfile(
        id: Long,
        name: String? = null,
        colorSeed: Long? = null,
        position: Long? = null,
        requiresAuth: Boolean? = null,
        isArchived: Boolean? = null,
    ) {
        handler.await {
            profilesQueries.update(
                id = id,
                name = name,
                colorSeed = colorSeed,
                position = position,
                requiresAuth = requiresAuth,
                isArchived = isArchived,
            )
        }
    }

    suspend fun deleteProfile(id: Long) {
        handler.await {
            profilesQueries.delete(id)
        }
    }

    suspend fun clearProfileData(profileId: Long) {
        handler.await(inTransaction = true) {
            download_preferencesQueries.deleteByProfile(profileId)
            playback_preferencesQueries.deleteByProfile(profileId)
            playback_stateQueries.deleteByProfile(profileId)
            entries_categoriesQueries.deleteByProfile(profileId)
            merged_entriesQueries.deleteByProfile(profileId)
            historyQueries.deleteByProfile(profileId)
            excluded_scanlatorsQueries.deleteByProfile(profileId)
            entry_syncQueries.deleteByProfile(profileId)
            chaptersQueries.deleteByProfile(profileId)
            categoriesQueries.deleteByProfile(profileId)
            entriesQueries.deleteByProfile(profileId)
        }
    }

    suspend fun getVisibleProfileCount(): Long {
        return handler.awaitOne {
            profilesQueries.getVisibleProfileCount()
        }
    }

    suspend fun getMangaCount(profileId: Long): Long {
        return handler.awaitOne {
            entriesQueries.countProfileFavorites(profileId)
        }
    }

    suspend fun getAllCategories(profileId: Long): List<Category> {
        return handler.awaitList {
            categoriesQueries.getCategories(profileId, ::mapCategory)
        }
    }

    private fun mapProfile(
        id: Long,
        uuid: String,
        name: String,
        colorSeed: Long,
        position: Long,
        requiresAuth: Boolean,
        isArchived: Boolean,
        createdAt: Long,
        updatedAt: Long,
    ): Profile {
        @Suppress("UNUSED_VARIABLE")
        val ignored = createdAt + updatedAt
        return Profile(
            id = id,
            uuid = uuid,
            name = name,
            colorSeed = colorSeed,
            position = position,
            requiresAuth = requiresAuth,
            isArchived = isArchived,
        )
    }

    private fun mapCategory(
        id: Long,
        name: String,
        order: Long,
        flags: Long,
    ): Category {
        return Category(
            id = id,
            name = name,
            order = order,
            flags = flags,
        )
    }
}
