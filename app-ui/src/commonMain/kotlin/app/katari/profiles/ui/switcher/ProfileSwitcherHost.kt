package app.katari.profiles.ui.switcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.katari.core.platform.currentPlatformContext
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.launch

/**
 * Hosts the profile switcher sheet for [content], which opens it through [LocalProfileSwitcher]. [onManageProfiles]
 * opens profile management once the sheet has closed.
 */
@Composable
fun ProfileSwitcherHost(
    navigator: Navigator,
    onManageProfiles: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = currentPlatformContext()
    val graph = LocalAppUiGraph.current
    val profileManager = graph.profileManager
    val scope = rememberCoroutineScope()
    val profiles by profileManager.visibleProfiles.collectAsState()
    val activeProfile by profileManager.activeProfile.collectAsState()
    var showSheet by rememberSaveable { mutableStateOf(false) }

    val current = activeProfile
    val switcher = remember(current, profiles.size) {
        current?.takeIf { profiles.size > 1 }?.let { ProfileSwitcher(it, profiles.size) { showSheet = true } }
    }
    CompositionLocalProvider(LocalProfileSwitcher provides switcher, content)

    if (showSheet && current != null && switcher != null) {
        ProfileSwitcherSheet(
            profiles = profiles,
            activeProfileId = current.id,
            isLocked = { profileManager.profileRequiresUnlock(it.id) },
            onSelect = { profile ->
                if (profile.id == current.id) {
                    showSheet = false
                } else {
                    scope.launch {
                        switchToProfile(
                            context = context,
                            profileManager = profileManager,
                            profileUnlock = graph.profileUnlock,
                            switchEffects = graph.profileSwitchEffects,
                            profile = profile,
                            onBeforeSwitch = {
                                showSheet = false
                                navigator.popUntilRoot()
                            },
                        )
                    }
                }
            },
            onManageProfiles = {
                showSheet = false
                onManageProfiles()
            },
            onDismissRequest = { showSheet = false },
        )
    }
}
