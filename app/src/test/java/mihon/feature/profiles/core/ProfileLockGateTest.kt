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
    fun `protected profile without a recorded deactivation requires authentication`() {
        val gate = gate()

        gate.requiresAuthNow(LOCKED_PROFILE_ID) shouldBe true
    }

    @Test
    fun `profile deactivated within its idle window is entered without authentication`() {
        val gate = gate(lockDelayMinutes = 10)
        gate.markAuthenticated(LOCKED_PROFILE_ID)
        gate.markInactive(LOCKED_PROFILE_ID)
        currentTime += 9 * MINUTE_MILLIS

        gate.requiresAuthNow(LOCKED_PROFILE_ID) shouldBe false
    }

    @Test
    fun `profile deactivated beyond its idle window requires authentication`() {
        val gate = gate(lockDelayMinutes = 10)
        gate.markAuthenticated(LOCKED_PROFILE_ID)
        gate.markInactive(LOCKED_PROFILE_ID)
        currentTime += 11 * MINUTE_MILLIS

        gate.requiresAuthNow(LOCKED_PROFILE_ID) shouldBe true
    }

    @Test
    fun `always delay requires authentication immediately after deactivation`() {
        val gate = gate(lockDelayMinutes = 0)
        gate.markAuthenticated(LOCKED_PROFILE_ID)
        gate.markInactive(LOCKED_PROFILE_ID)

        gate.requiresAuthNow(LOCKED_PROFILE_ID) shouldBe true
    }

    @Test
    fun `never delay enters without authentication however long the profile was inactive`() {
        val gate = gate(lockDelayMinutes = -1)
        gate.markAuthenticated(LOCKED_PROFILE_ID)
        gate.markInactive(LOCKED_PROFILE_ID)
        currentTime += 30 * 24 * HOUR_MILLIS

        gate.requiresAuthNow(LOCKED_PROFILE_ID) shouldBe false
    }

    @Test
    fun `profile without a biometric lock never requires authentication`() {
        val gate = gate(useAuthenticator = false)

        gate.requiresAuthNow(LOCKED_PROFILE_ID) shouldBe false
    }

    @Test
    fun `deactivating a profile that was never authenticated in this process keeps it locked`() {
        val gate = gate(lockDelayMinutes = 10)
        gate.markInactive(LOCKED_PROFILE_ID)

        gate.requiresAuthNow(LOCKED_PROFILE_ID) shouldBe true
    }

    @Test
    fun `active profile is entered without authentication`() {
        val gate = gate(lockDelayMinutes = 0, activeProfileId = LOCKED_PROFILE_ID)

        gate.requiresAuthNow(LOCKED_PROFILE_ID) shouldBe false
    }

    private fun gate(
        useAuthenticator: Boolean = true,
        lockDelayMinutes: Int = 10,
        activeProfileId: Long = OPEN_PROFILE_ID,
    ): ProfileLockGate {
        val stores = mapOf<Long, PreferenceStore>(
            OPEN_PROFILE_ID to InMemoryPreferenceStore(),
            LOCKED_PROFILE_ID to lockedProfileStore(useAuthenticator, lockDelayMinutes),
        )
        val profileStore = mockk<ProfileStore>()
        every { profileStore.currentProfileId } answers { activeProfileId }
        every { profileStore.profileStore(any()) } answers { stores.getValue(firstArg()) }
        return ProfileLockGate(profileStore, now = { currentTime })
    }

    private fun lockedProfileStore(useAuthenticator: Boolean, lockDelayMinutes: Int): PreferenceStore {
        if (!useAuthenticator) return InMemoryPreferenceStore()
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
