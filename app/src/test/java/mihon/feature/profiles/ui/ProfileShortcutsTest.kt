package mihon.feature.profiles.ui

import androidx.fragment.app.FragmentActivity
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.authenticate
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.ProfileManager
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.Preference

class ProfileShortcutsTest {

    @Test
    fun `switching to protected profile clears pending app unlock after authentication`() = runTest {
        val activity = mockk<FragmentActivity>(relaxed = true)
        val profileManager = mockk<ProfileManager>()
        val uiPreferences = mockk<UiPreferences>()
        val themeMode = mockk<Preference<ThemeMode>>()
        val profile = profile(id = 2L, name = "Work")

        every { profileManager.profileRequiresUnlock(profile.id) } returns true
        coEvery { profileManager.setActiveProfile(profile.id) } just runs
        every { uiPreferences.themeMode } returns themeMode
        every { themeMode.get() } returns ThemeMode.SYSTEM
        mockkObject(AuthenticatorUtil)
        mockkStatic(::setAppCompatDelegateThemeMode)
        coEvery {
            AuthenticatorUtil.run {
                activity.authenticate(any(), any())
            }
        } returns true
        every { setAppCompatDelegateThemeMode(any()) } just runs

        SecureActivityDelegate.requireUnlock = true
        try {
            switchToProfile(
                context = activity,
                profileManager = profileManager,
                uiPreferences = uiPreferences,
                profile = profile,
            ) shouldBe true

            SecureActivityDelegate.requireUnlock shouldBe false
        } finally {
            SecureActivityDelegate.requireUnlock = true
            unmockkAll()
        }
    }

    @Test
    fun `returns other profile when exactly two profiles exist`() {
        val profiles = listOf(
            profile(id = 1L, name = "Default"),
            profile(id = 2L, name = "Work"),
        )

        resolveProfileShortcutTarget(profiles, activeProfileId = 1L)?.id shouldBe 2L
    }

    @Test
    fun `returns null when active profile is missing`() {
        val profiles = listOf(
            profile(id = 1L, name = "Default"),
            profile(id = 2L, name = "Work"),
        )

        resolveProfileShortcutTarget(profiles, activeProfileId = null).shouldBeNull()
    }

    @Test
    fun `returns null when profile count is not two`() {
        resolveProfileShortcutTarget(
            profiles = listOf(profile(id = 1L, name = "Default")),
            activeProfileId = 1L,
        ).shouldBeNull()

        resolveProfileShortcutTarget(
            profiles = listOf(
                profile(id = 1L, name = "Default"),
                profile(id = 2L, name = "Work"),
                profile(id = 3L, name = "Guest"),
            ),
            activeProfileId = 1L,
        ).shouldBeNull()
    }

    @Test
    fun `returns null when active profile is not in visible profiles`() {
        val profiles = listOf(
            profile(id = 1L, name = "Default"),
            profile(id = 2L, name = "Work"),
        )

        resolveProfileShortcutTarget(profiles, activeProfileId = 3L).shouldBeNull()
    }

    private fun profile(id: Long, name: String) = Profile(
        id = id,
        uuid = "uuid-$id",
        name = name,
        colorSeed = 0L,
        position = id,
        requiresAuth = false,
        isArchived = false,
    )
}
