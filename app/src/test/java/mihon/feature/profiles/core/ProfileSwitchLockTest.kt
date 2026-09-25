package mihon.feature.profiles.core

import android.app.Application
import eu.kanade.tachiyomi.extension.ExtensionManager
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.lifecycle.removal.EntryDestructiveRemovalFeature
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry
import tachiyomi.domain.entry.repository.EntryRepository

/**
 * Wiring of the profile idle clock into profile activation: switching away must start (or fail to start)
 * the leaving profile's "Lock when idle" window.
 */
class ProfileSwitchLockTest {

    @Test
    fun `switching away from an authenticated profile starts its idle window`() = runTest {
        val manager = profileSwitchLockManager()

        manager.setActiveProfile(LOCKED_PROFILE_ID, rescheduleJobs = false)
        manager.markProfileAuthenticated(LOCKED_PROFILE_ID)
        manager.setActiveProfile(ProfileConstants.DEFAULT_PROFILE_ID, rescheduleJobs = false)

        manager.profileRequiresAuthNow(LOCKED_PROFILE_ID) shouldBe false
    }

    @Test
    fun `switching away from a profile that was never authenticated keeps it locked`() = runTest {
        val manager = profileSwitchLockManager()

        manager.setActiveProfile(LOCKED_PROFILE_ID, rescheduleJobs = false)
        manager.setActiveProfile(ProfileConstants.DEFAULT_PROFILE_ID, rescheduleJobs = false)

        manager.profileRequiresAuthNow(LOCKED_PROFILE_ID) shouldBe true
    }

    private fun profileSwitchLockManager(): ProfileManager {
        val openProfile = Profile(
            id = ProfileConstants.DEFAULT_PROFILE_ID,
            uuid = ProfileConstants.DEFAULT_PROFILE_UUID,
            name = ProfileConstants.DEFAULT_PROFILE_NAME,
            colorSeed = 0L,
            position = 0L,
            requiresAuth = false,
            isArchived = false,
        )
        val lockedProfile = Profile(
            id = LOCKED_PROFILE_ID,
            uuid = "profile-$LOCKED_PROFILE_ID",
            name = "Private",
            colorSeed = 1L,
            position = 1L,
            requiresAuth = false,
            isArchived = false,
        )
        val profileStores = mutableMapOf<Long, PreferenceStore>(
            LOCKED_PROFILE_ID to lockedProfileStore(),
        )
        val profileDatabase = mockk<ProfileDatabase>()
        val profileStore = mockk<ProfileStoreImpl>()
        var currentId = ProfileConstants.DEFAULT_PROFILE_ID

        coEvery { profileDatabase.subscribeProfiles(any()) } returns flowOf(listOf(openProfile, lockedProfile))
        coEvery { profileDatabase.getProfileById(ProfileConstants.DEFAULT_PROFILE_ID) } returns openProfile
        coEvery { profileDatabase.getProfileById(LOCKED_PROFILE_ID) } returns lockedProfile
        every { profileStore.currentProfileId } answers { currentId }
        every { profileStore.setCurrentProfileId(any()) } answers { currentId = firstArg() }
        every { profileStore.profileStore(any()) } answers {
            profileStores.getOrPut(firstArg()) { InMemoryPreferenceStore() }
        }

        return ProfileManager(
            application = mockk<Application>(relaxed = true),
            profileDatabase = profileDatabase,
            profileStore = profileStore,
            profilesPreferences = ProfilesPreferences(InMemoryPreferenceStore()),
            extensionManager = mockk<ExtensionManager>(relaxed = true),
            preferenceOwnership = ProfilePreferenceOwnership(ProfilePreferenceOwnerRegistry()),
            entryRepository = mockk<EntryRepository>(relaxed = true),
            destructiveRemoval = mockk<EntryDestructiveRemovalFeature>(relaxed = true),
        )
    }

    private fun lockedProfileStore(): PreferenceStore {
        return InMemoryPreferenceStore(
            sequenceOf(
                InMemoryPreferenceStore.InMemoryPreference("use_biometric_lock", true, false),
                InMemoryPreferenceStore.InMemoryPreference("lock_app_after", 10, 0),
            ),
        )
    }

    private companion object {
        const val LOCKED_PROFILE_ID = 2L
    }
}
