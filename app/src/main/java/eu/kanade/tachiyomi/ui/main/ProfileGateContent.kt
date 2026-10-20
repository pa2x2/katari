package eu.kanade.tachiyomi.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.ui.ProfilePickerScene
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ProfileGateContent(
    state: ProfileStartupGateState,
    profiles: List<Profile>,
    activeProfileId: Long?,
    isLocked: (Profile) -> Boolean,
    authProfileName: String?,
    onProfileSelected: (Profile) -> Unit,
) {
    when {
        state == ProfileStartupGateState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator()
            }
        }
        state == ProfileStartupGateState.Authenticating -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
                ) {
                    LoadingIndicator()
                    authProfileName?.let {
                        Text(
                            text = stringResource(MR.strings.unlock_app_title, it),
                            role = TextRole.ItemTitle,
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
                    LoadingIndicator()
                }
            } else {
                ProfilePickerScene(
                    profiles = profiles,
                    activeProfileId = activeProfileId,
                    isLocked = isLocked,
                    onProfileSelected = onProfileSelected,
                )
            }
        }
        else -> Unit
    }
}
