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

/**
 * Where the app starts. [canAuthenticate] is false where the platform cannot ask the user to authenticate: a locked
 * profile then never opens, and the picker lets the user choose another.
 */
fun resolveInitialStartupGateDecision(
    shouldShowPicker: Boolean,
    initialProfile: Profile?,
    requiresProfileUnlock: Boolean,
    shouldSkipProfileAuth: Boolean,
    canAuthenticate: Boolean = true,
): ProfileStartupDecision {
    return when {
        shouldShowPicker || (initialProfile != null && requiresProfileUnlock && !canAuthenticate) ->
            ProfileStartupDecision(
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

/** Where a picker showing only [profile] goes; as in [resolveInitialStartupGateDecision] for [canAuthenticate]. */
fun resolvePickerCollapseStartupGateDecision(
    profile: Profile?,
    requiresProfileUnlock: Boolean,
    shouldSkipProfileAuth: Boolean,
    canAuthenticate: Boolean = true,
): ProfileStartupDecision {
    return if (profile != null && requiresProfileUnlock && !canAuthenticate) {
        ProfileStartupDecision(
            allowAppUnlockPrompt = false,
            state = ProfileStartupGateState.Picker,
            pendingAuthProfile = null,
        )
    } else if (profile != null && requiresProfileUnlock && !shouldSkipProfileAuth) {
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
