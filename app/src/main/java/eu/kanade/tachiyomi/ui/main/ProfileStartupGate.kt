package eu.kanade.tachiyomi.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.ui.ProfilePickerScene
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

internal data class ProfileStartupDecision(
    val allowAppUnlockPrompt: Boolean,
    val state: ProfileStartupGateState,
    val pendingAuthProfile: Profile?,
)

internal data class StartupRestorationDecision(
    val shouldResumeStartup: Boolean,
    val allowAppUnlockPrompt: Boolean,
)

internal fun resolveStartupRestorationDecision(
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

internal fun resolveInitialStartupGateDecision(
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

internal fun resolvePickerCollapseStartupGateDecision(
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

internal enum class ProfileStartupGateState {
    Loading,
    Picker,
    Authenticating,
    Ready,
}

@Composable
internal fun ProfileGateContent(
    state: ProfileStartupGateState,
    profiles: List<Profile>,
    activeProfileId: Long?,
    authProfileName: String?,
    onProfileSelected: (Profile) -> Unit,
) {
    when {
        state == ProfileStartupGateState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        state == ProfileStartupGateState.Authenticating -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    authProfileName?.let {
                        Text(
                            text = stringResource(MR.strings.unlock_app_title, it),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
        state == ProfileStartupGateState.Picker -> {
            if (profiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                ProfilePickerScene(
                    profiles = profiles,
                    activeProfileId = activeProfileId,
                    onProfileSelected = onProfileSelected,
                    onOpenManagement = null,
                )
            }
        }
        else -> Unit
    }
}
