package mihon.feature.profiles.core

import android.app.Application
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.adapter.LegacyMangaSourceAdapter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.lifecycle.removal.EntryDestructiveRemovalFeature
import mihon.entry.interactions.lifecycle.removal.EntryDestructiveRemovalResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository

class ProfileManagerTest {

    @Test
    fun `permanent deletion removes profile entries through the destructive removal Feature`() = runTest {
        val profileId = 2L
        val profile = Profile(
            id = profileId,
            uuid = "profile-$profileId",
            name = "Archived",
            colorSeed = 1L,
            position = 1L,
            requiresAuth = false,
            isArchived = true,
        )
        val entries = listOf(Entry.create().copy(id = 7L, profileId = profileId))
        val profileDatabase = mockk<ProfileDatabase> {
            coEvery { subscribeProfiles(any()) } returns flowOf(listOf(defaultProfile(), profile))
            coEvery { getProfileById(profileId) } returns profile
            coEvery { getProfileById(ProfileConstants.DEFAULT_PROFILE_ID) } returns defaultProfile()
            coEvery { deleteProfile(profileId) } just runs
        }
        val profileStore = mockk<ProfileStoreImpl> {
            every { currentProfileId } returns ProfileConstants.DEFAULT_PROFILE_ID
            every { deleteProfileState(profileId) } just runs
        }
        val entryRepository = mockk<EntryRepository> {
            coEvery { getAllEntriesByProfile(profileId) } returns entries
        }
        val destructiveRemoval = mockk<EntryDestructiveRemovalFeature> {
            coEvery { remove(entries) } returns EntryDestructiveRemovalResult.Removed(entries, emptyList())
        }
        val manager = ProfileManager(
            application = mockk(relaxed = true),
            profileDatabase = profileDatabase,
            profileStore = profileStore,
            profilesPreferences = ProfilesPreferences(InMemoryPreferenceStore()),
            extensionManager = mockk(relaxed = true),
            preferenceOwnership = ProfilePreferenceOwnership(ProfilePreferenceOwnerRegistry()),
            entryRepository = entryRepository,
            destructiveRemoval = destructiveRemoval,
        )

        manager.permanentlyDeleteProfile(profileId)

        coVerifyOrder {
            entryRepository.getAllEntriesByProfile(profileId)
            destructiveRemoval.remove(entries)
            profileDatabase.deleteProfile(profileId)
        }
        verify(exactly = 1) { profileStore.deleteProfileState(profileId) }
    }

    @Test
    fun `permanent deletion preserves profile when destructive removal fails transactionally`() = runTest {
        val profileId = 2L
        val profile = Profile(
            id = profileId,
            uuid = "profile-$profileId",
            name = "Archived",
            colorSeed = 1L,
            position = 1L,
            requiresAuth = false,
            isArchived = true,
        )
        val entries = listOf(Entry.create().copy(id = 7L, profileId = profileId))
        val failure = IllegalStateException("removal failed")
        val profileDatabase = mockk<ProfileDatabase> {
            coEvery { subscribeProfiles(any()) } returns flowOf(listOf(defaultProfile(), profile))
            coEvery { getProfileById(profileId) } returns profile
            coEvery { getProfileById(ProfileConstants.DEFAULT_PROFILE_ID) } returns defaultProfile()
        }
        val profileStore = mockk<ProfileStoreImpl> {
            every { currentProfileId } returns ProfileConstants.DEFAULT_PROFILE_ID
        }
        val entryRepository = mockk<EntryRepository> {
            coEvery { getAllEntriesByProfile(profileId) } returns entries
        }
        val destructiveRemoval = mockk<EntryDestructiveRemovalFeature> {
            coEvery { remove(entries) } returns EntryDestructiveRemovalResult.Failed(entries, failure)
        }
        val manager = ProfileManager(
            application = mockk(relaxed = true),
            profileDatabase = profileDatabase,
            profileStore = profileStore,
            profilesPreferences = ProfilesPreferences(InMemoryPreferenceStore()),
            extensionManager = mockk(relaxed = true),
            preferenceOwnership = ProfilePreferenceOwnership(ProfilePreferenceOwnerRegistry()),
            entryRepository = entryRepository,
            destructiveRemoval = destructiveRemoval,
        )

        assertThrows<IllegalStateException> {
            manager.permanentlyDeleteProfile(profileId)
        }

        coVerifyOrder {
            entryRepository.getAllEntriesByProfile(profileId)
            destructiveRemoval.remove(entries)
        }
        coVerify(exactly = 0) { profileDatabase.deleteProfile(profileId) }
        verify(exactly = 0) { profileStore.deleteProfileState(profileId) }
    }

    private fun defaultProfile() = Profile(
        id = ProfileConstants.DEFAULT_PROFILE_ID,
        uuid = ProfileConstants.DEFAULT_PROFILE_UUID,
        name = ProfileConstants.DEFAULT_PROFILE_NAME,
        colorSeed = 0L,
        position = 0L,
        requiresAuth = false,
        isArchived = false,
    )
}
