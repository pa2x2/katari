package mihon.feature.profiles.core

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.lifecycle.removal.EntryDestructiveRemovalFeature
import mihon.entry.interactions.lifecycle.removal.EntryDestructiveRemovalResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository

class ProfileManagerTest {

    private val profileId = 2L
    private val archivedProfile = Profile(
        id = profileId,
        uuid = "profile-$profileId",
        name = "Archived",
        colorSeed = 1L,
        position = 1L,
        requiresAuth = false,
        isArchived = true,
    )
    private val defaultProfile = Profile(
        id = ProfileConstants.DEFAULT_PROFILE_ID,
        uuid = ProfileConstants.DEFAULT_PROFILE_UUID,
        name = ProfileConstants.DEFAULT_PROFILE_NAME,
        colorSeed = 0L,
        position = 0L,
        requiresAuth = false,
        isArchived = false,
    )
    private val entries = listOf(Entry.create().copy(id = 7L, profileId = profileId))

    private val profileDatabase = mockk<ProfileDatabase> {
        coEvery { subscribeProfiles(any()) } returns flowOf(listOf(defaultProfile, archivedProfile))
        coEvery { getProfileById(profileId) } returns archivedProfile
        coEvery { getProfileById(ProfileConstants.DEFAULT_PROFILE_ID) } returns defaultProfile
        coEvery { deleteProfile(profileId) } just runs
    }
    private val profileStore = mockk<ProfileStoreImpl> {
        every { currentProfileId } returns ProfileConstants.DEFAULT_PROFILE_ID
        every { deleteProfileState(profileId) } just runs
    }
    private val entryRepository = mockk<EntryRepository> {
        coEvery { getAllEntriesByProfile(profileId) } returns entries
    }
    private val destructiveRemoval = mockk<EntryDestructiveRemovalFeature>()

    private fun manager() = ProfileManager(
        application = mockk(relaxed = true),
        profileDatabase = profileDatabase,
        profileStore = profileStore,
        profilesPreferences = ProfilesPreferences(InMemoryPreferenceStore()),
        extensionManager = mockk(relaxed = true),
        preferenceOwnership = ProfilePreferenceOwnership(ProfilePreferenceOwnerRegistry()),
        entryRepository = entryRepository,
        destructiveRemoval = destructiveRemoval,
    )

    @Test
    fun `permanent deletion preserves profile when destructive removal fails transactionally`() = runTest {
        coEvery { destructiveRemoval.remove(entries) } returns
            EntryDestructiveRemovalResult.Failed(entries, IllegalStateException("removal failed"))

        assertThrows<IllegalStateException> {
            manager().permanentlyDeleteProfile(profileId)
        }

        coVerify(exactly = 0) { profileDatabase.deleteProfile(profileId) }
        verify(exactly = 0) { profileStore.deleteProfileState(profileId) }
    }
}
