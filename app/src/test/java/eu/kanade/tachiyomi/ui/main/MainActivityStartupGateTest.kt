package eu.kanade.tachiyomi.ui.main

import io.kotest.matchers.shouldBe
import mihon.feature.profiles.core.Profile
import org.junit.jupiter.api.Test

class MainActivityStartupGateTest {

    @Test
    fun `startup restoration resumes pending picker flow after recreation`() {
        resolveStartupRestorationDecision(
            startupCompleted = false,
            restoredAllowAppUnlockPrompt = false,
            shouldShowPickerOnLaunch = false,
        ) shouldBe StartupRestorationDecision(
            shouldResumeStartup = true,
            allowAppUnlockPrompt = false,
        )
    }

    @Test
    fun `initial startup authenticates locked profile when picker is skipped`() {
        val profile = profile(id = 2L)

        resolveInitialStartupGateDecision(
            shouldShowPicker = false,
            initialProfile = profile,
            requiresProfileUnlock = true,
            shouldSkipProfileAuth = false,
        ) shouldBe ProfileStartupDecision(
            allowAppUnlockPrompt = true,
            state = ProfileStartupGateState.Authenticating,
            pendingAuthProfile = profile,
        )
    }

    private fun profile(id: Long) = Profile(
        id = id,
        uuid = "uuid-$id",
        name = "Profile $id",
        colorSeed = 0L,
        position = id,
        requiresAuth = false,
        isArchived = false,
    )
}
