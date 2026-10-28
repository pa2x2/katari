package eu.kanade.tachiyomi.ui.main

import mihon.feature.profiles.core.Profile

data class ProfileStartupDecision(
    val allowAppUnlockPrompt: Boolean,
    val state: ProfileStartupGateState,
    val pendingAuthProfile: Profile?,
)

data class StartupRestorationDecision(
    val shouldResumeStartup: Boolean,
    val allowAppUnlockPrompt: Boolean,
)

fun resolveStartupRestorationDecision(
    startupCompleted: Boolean,
    restoredAllowAppUnlockPrompt: Boolean?,
    shouldShowPickerOnLaunch: Boolean,
): StartupRestorationDecision {
    return StartupRestorationDecision(
        shouldResumeStartup = !startupCompleted,
        allowAppUnlockPrompt = when {
            startupCompleted -> true
            restoredAllowAppUnlockPrompt != null -> restoredAllowAppUnlockPrompt
            else -> !shouldShowPickerOnLaunch
        },
    )
}

fun resolveInitialStartupGateDecision(
    shouldShowPicker: Boolean,
    initialProfile: Profile?,
    requiresProfileUnlock: Boolean,
    shouldSkipProfileAuth: Boolean,
): ProfileStartupDecision {
    return when {
        shouldShowPicker -> ProfileStartupDecision(
            allowAppUnlockPrompt = false,
            state = ProfileStartupGateState.Picker,
            pendingAuthProfile = null,
        )
        initialProfile != null && requiresProfileUnlock && !shouldSkipProfileAuth -> ProfileStartupDecision(
            allowAppUnlockPrompt = true,
            state = ProfileStartupGateState.Authenticating,
            pendingAuthProfile = initialProfile,
        )
        else -> ProfileStartupDecision(
            allowAppUnlockPrompt = true,
            state = ProfileStartupGateState.Ready,
            pendingAuthProfile = null,
        )
    }
}

fun resolvePickerCollapseStartupGateDecision(
    profile: Profile?,
    requiresProfileUnlock: Boolean,
    shouldSkipProfileAuth: Boolean,
): ProfileStartupDecision {
    return if (profile != null && requiresProfileUnlock && !shouldSkipProfileAuth) {
        ProfileStartupDecision(
            allowAppUnlockPrompt = true,
            state = ProfileStartupGateState.Authenticating,
            pendingAuthProfile = profile,
        )
    } else {
        ProfileStartupDecision(
            allowAppUnlockPrompt = true,
            state = ProfileStartupGateState.Ready,
            pendingAuthProfile = null,
        )
    }
}

enum class ProfileStartupGateState {
    Loading,
    Picker,
    Authenticating,
    Ready,
}
