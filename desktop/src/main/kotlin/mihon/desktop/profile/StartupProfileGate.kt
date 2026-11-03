package mihon.desktop.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.ui.main.ProfileGateContent
import eu.kanade.tachiyomi.ui.main.ProfileStartupGateState
import eu.kanade.tachiyomi.ui.main.resolveInitialStartupGateDecision
import eu.kanade.tachiyomi.ui.main.resolvePickerCollapseStartupGateDecision
import kotlinx.coroutines.launch
import mihon.app.ui.di.LocalAppUiGraph
import mihon.feature.profiles.core.ProfileStartupSnapshot

/**
 * Shows [content] once a profile is open, after the profile picker when the app asks for one at start. Desktop can't
 * ask the user to authenticate, so a locked profile never opens: when it would be the first, the picker shows instead,
 * and picking a locked profile there does nothing. With only locked profiles the picker stays. [startup] is what the
 * app loaded before the window opened.
 */
@Composable
fun StartupProfileGate(startup: ProfileStartupSnapshot, content: @Composable () -> Unit) {
    val profileManager = LocalAppUiGraph.current.profileManager
    val scope = rememberCoroutineScope()
    val visibleProfiles by profileManager.visibleProfiles.collectAsState()
    val activeProfile by profileManager.activeProfile.collectAsState()
    val gateProfiles = visibleProfiles.ifEmpty { startup.visibleProfiles }
    var gateState by remember {
        mutableStateOf(
            resolveInitialStartupGateDecision(
                shouldShowPicker = startup.shouldShowPicker,
                initialProfile = startup.initialProfile,
                requiresProfileUnlock = startup.initialProfile
                    ?.let { profileManager.profileRequiresUnlock(it.id) } == true,
                shouldSkipProfileAuth = false,
                canAuthenticate = false,
            ).state,
        )
    }

    LaunchedEffect(gateState, gateProfiles, activeProfile?.id) {
        if (gateState == ProfileStartupGateState.Picker && gateProfiles.size == 1) {
            val profile = activeProfile ?: gateProfiles.firstOrNull()
            gateState = resolvePickerCollapseStartupGateDecision(
                profile = profile,
                requiresProfileUnlock = profile?.let { profileManager.profileRequiresUnlock(it.id) } == true,
                shouldSkipProfileAuth = false,
                canAuthenticate = false,
            ).state
        }
    }

    if (gateState == ProfileStartupGateState.Ready) {
        content()
    } else {
        ProfileGateContent(
            state = gateState,
            profiles = gateProfiles,
            activeProfileId = activeProfile?.id,
            isLocked = { profileManager.profileRequiresUnlock(it.id) },
            authProfileName = null,
            onProfileSelected = { profile ->
                if (!profileManager.profileRequiresUnlock(profile.id)) {
                    scope.launch {
                        profileManager.setActiveProfile(profile.id)
                        gateState = ProfileStartupGateState.Ready
                    }
                }
            },
        )
    }
}
