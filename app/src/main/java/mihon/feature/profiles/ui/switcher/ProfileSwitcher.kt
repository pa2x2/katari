package mihon.feature.profiles.ui.switcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.feature.profiles.core.Profile

/**
 * How profile UI on the home screen reaches the profile switcher. [LocalProfileSwitcher] is null outside the home
 * screen and with a single visible profile, where there is nothing to switch to and no profile UI is shown.
 */
@Immutable
internal class ProfileSwitcher(
    val activeProfile: Profile,
    val profileCount: Int,
    val open: () -> Unit,
)

internal val LocalProfileSwitcher = compositionLocalOf<ProfileSwitcher?> { null }

/** Hosts the profile switcher sheet for [content], which opens it through [LocalProfileSwitcher]. */
@Composable
internal fun ProfileSwitcherHost(
    navigator: Navigator,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val profileManager = remember { context.appGraph.profileManager }
    val uiPreferences = remember { context.appGraph.uiPreferences }
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
                            uiPreferences = uiPreferences,
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
                navigator.push(SettingsScreen(SettingsScreen.Destination.Profiles))
            },
            onDismissRequest = { showSheet = false },
        )
    }
}
