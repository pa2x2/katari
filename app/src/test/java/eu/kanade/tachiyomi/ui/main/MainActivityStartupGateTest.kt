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
    fun `startup restoration skips startup flow after completion`() {
        resolveStartupRestorationDecision(
            startupCompleted = true,
            restoredAllowAppUnlockPrompt = false,
            shouldShowPickerOnLaunch = true,
        ) shouldBe StartupRestorationDecision(
            shouldResumeStartup = false,
            allowAppUnlockPrompt = true,
        )
    }

    @Test
    fun `startup restoration derives unlock prompt from picker visibility on fresh launch`() {
        resolveStartupRestorationDecision(
            startupCompleted = false,
            restoredAllowAppUnlockPrompt = null,
            shouldShowPickerOnLaunch = true,
        ) shouldBe StartupRestorationDecision(
            shouldResumeStartup = true,
            allowAppUnlockPrompt = false,
        )
    }

    @Test
    fun `initial startup shows picker before profile auth`() {
        val profile = profile(id = 2L)

        resolveInitialStartupGateDecision(
            shouldShowPicker = true,
            initialProfile = profile,
            requiresProfileUnlock = true,
            shouldSkipProfileAuth = false,
        ) shouldBe ProfileStartupDecision(
            allowAppUnlockPrompt = false,
            state = ProfileStartupGateState.Picker,
            pendingAuthProfile = null,
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

    @Test
    fun `initial startup skips duplicate profile auth when app unlock already covers it`() {
        val profile = profile(id = 1L)

        resolveInitialStartupGateDecision(
            shouldShowPicker = false,
            initialProfile = profile,
            requiresProfileUnlock = true,
            shouldSkipProfileAuth = true,
        ) shouldBe ProfileStartupDecision(
            allowAppUnlockPrompt = true,
            state = ProfileStartupGateState.Ready,
            pendingAuthProfile = null,
        )
    }

    @Test
    fun `picker collapse authenticates remaining locked profile`() {
        val profile = profile(id = 3L)

        resolvePickerCollapseStartupGateDecision(
            profile = profile,
            requiresProfileUnlock = true,
            shouldSkipProfileAuth = false,
        ) shouldBe ProfileStartupDecision(
            allowAppUnlockPrompt = true,
            state = ProfileStartupGateState.Authenticating,
            pendingAuthProfile = profile,
        )
    }

    @Test
    fun `picker collapse becomes ready when remaining profile does not require auth`() {
        resolvePickerCollapseStartupGateDecision(
            profile = profile(id = 4L),
            requiresProfileUnlock = false,
            shouldSkipProfileAuth = false,
        ) shouldBe ProfileStartupDecision(
            allowAppUnlockPrompt = true,
            state = ProfileStartupGateState.Ready,
            pendingAuthProfile = null,
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
