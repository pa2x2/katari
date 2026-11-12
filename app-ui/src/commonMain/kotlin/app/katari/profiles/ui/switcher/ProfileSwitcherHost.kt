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
import app.katari.presentation.kit.sheet.PopoverAnchor
import app.katari.presentation.kit.sheet.rememberPopoverAnchor
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.launch

/**
 * Hosts the profile switcher for [content], which opens it through [LocalProfileSwitcher]. [onManageProfiles]
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
    // Which button opened it; after the activity is recreated the switcher opens as a sheet
    var openedFrom by remember { mutableStateOf<PopoverAnchor?>(null) }
    val dockAnchor = rememberPopoverAnchor()

    val current = activeProfile
    val switcher = remember(current, profiles) {
        // The open profile can be archived on another device, which leaves it out of the profiles to switch to
        current?.takeIf { profiles.any { profile -> profile.id != it.id } }?.let {
            ProfileSwitcher(it, profiles.size, dockAnchor) { from ->
                openedFrom = from
                showSheet = true
            }
        }
    }
    CompositionLocalProvider(LocalProfileSwitcher provides switcher, content)

    if (showSheet && current != null && switcher != null) {
        ProfileSwitcherPopover(
            anchor = openedFrom,
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
