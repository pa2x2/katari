package mihon.feature.profiles.core

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore

class ProfileLockGateTest {

    private var currentTime = INITIAL_TIME

    @Test
    fun `idle window decides whether an authenticated profile must authenticate again`() {
        fun requiresAuthAfterDeactivation(lockDelayMinutes: Int, inactiveMillis: Long): Boolean {
            val gate = gate(lockDelayMinutes = lockDelayMinutes)
            gate.markAuthenticated(LOCKED_PROFILE_ID)
            gate.markInactive(LOCKED_PROFILE_ID)
            currentTime += inactiveMillis
            return gate.requiresAuthNow(LOCKED_PROFILE_ID)
        }

        requiresAuthAfterDeactivation(lockDelayMinutes = 10, inactiveMillis = 9 * MINUTE_MILLIS) shouldBe false
        requiresAuthAfterDeactivation(lockDelayMinutes = 10, inactiveMillis = 11 * MINUTE_MILLIS) shouldBe true
        requiresAuthAfterDeactivation(lockDelayMinutes = 0, inactiveMillis = 0L) shouldBe true
        requiresAuthAfterDeactivation(lockDelayMinutes = -1, inactiveMillis = 30 * 24 * HOUR_MILLIS) shouldBe false
    }

    @Test
    fun `protected profile never authenticated in this process stays locked`() {
        gate().requiresAuthNow(LOCKED_PROFILE_ID) shouldBe true

        val deactivated = gate(lockDelayMinutes = 10)
        deactivated.markInactive(LOCKED_PROFILE_ID)
        deactivated.requiresAuthNow(LOCKED_PROFILE_ID) shouldBe true
    }

    private fun gate(lockDelayMinutes: Int = 10): ProfileLockGate {
        val stores = mapOf<Long, PreferenceStore>(
            OPEN_PROFILE_ID to InMemoryPreferenceStore(),
            LOCKED_PROFILE_ID to lockedProfileStore(lockDelayMinutes),
        )
        val profileStore = mockk<ProfileStore>()
        every { profileStore.currentProfileId } returns OPEN_PROFILE_ID
        every { profileStore.profileStore(any()) } answers { stores.getValue(firstArg()) }
        return ProfileLockGate(profileStore, now = { currentTime })
    }

    private fun lockedProfileStore(lockDelayMinutes: Int): PreferenceStore {
        return InMemoryPreferenceStore(
            sequenceOf(
                InMemoryPreferenceStore.InMemoryPreference("use_biometric_lock", true, false),
                InMemoryPreferenceStore.InMemoryPreference("lock_app_after", lockDelayMinutes, 0),
            ),
        )
    }

    private companion object {
        const val OPEN_PROFILE_ID = 1L
        const val LOCKED_PROFILE_ID = 2L
        const val INITIAL_TIME = 1_700_000_000_000L
        const val MINUTE_MILLIS = 60_000L
        const val HOUR_MILLIS = 60 * MINUTE_MILLIS
    }
}
