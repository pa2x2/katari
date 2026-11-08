package app.katari.desktop.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.dialog.LockScreen
import app.katari.presentation.kit.feedback.ScreenMessagesHost
import app.katari.presentation.kit.feedback.ScreenMessagesOverlay
import app.katari.profiles.core.Profile
import app.katari.profiles.core.ProfileStartupSnapshot
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.tachiyomi.ui.main.ProfileGateContent
import eu.kanade.tachiyomi.ui.main.ProfileStartupGateState
import eu.kanade.tachiyomi.ui.main.resolveInitialStartupGateDecision
import eu.kanade.tachiyomi.ui.main.resolvePickerCollapseStartupGateDecision
import kotlinx.coroutines.launch

/**
 * Shows [content] once a profile is open, after the profile picker when the app asks for one at start, and asks for
 * the sign-in before a locked profile opens, over the picker, which stays when the sign-in is cancelled. When the app
 * would start straight into a locked profile, the picker shows with that profile's sign-in already asked. Once the
 * open profile is held locked again ([ActiveProfileLock]), the picker covers its screens, which keep their state under
 * it, and asks for its sign-in at once. [startup] is what the app loaded before the window opened.
 */
@Composable
fun WindowProfileGate(startup: ProfileStartupSnapshot, lock: ActiveProfileLock, content: @Composable () -> Unit) {
    val graph = LocalAppUiGraph.current
    val profileManager = graph.profileManager
    val profileUnlock = graph.profileUnlock
    val context = currentPlatformContext()
    val scope = rememberCoroutineScope()
    val visibleProfiles by profileManager.visibleProfiles.collectAsState()
    val activeProfile by profileManager.activeProfile.collectAsState()
    val held by lock.isHeld.collectAsState()
    val gateProfiles = visibleProfiles.ifEmpty { startup.visibleProfiles }
    val canAuthenticate = remember(context) { profileUnlock.isAvailable(context) }
    val start = remember {
        resolveInitialStartupGateDecision(
            shouldShowPicker = startup.shouldShowPicker,
            initialProfile = startup.initialProfile,
            requiresProfileUnlock = startup.initialProfile?.let { lock.requiresUnlock(it.id) } == true,
            shouldSkipProfileAuth = false,
            canAuthenticate = canAuthenticate,
        )
    }
    var unlocking by remember { mutableStateOf(start.pendingAuthProfile) }
    var opened by remember { mutableStateOf(start.state == ProfileStartupGateState.Ready) }
    LaunchedEffect(Unit) {
        if (opened) lock.release()
    }

    suspend fun open(profile: Profile) {
        if (profile.id != profileManager.activeProfileId) profileManager.setActiveProfile(profile.id)
        lock.release()
        opened = true
    }

    LaunchedEffect(unlocking) {
        val profile = unlocking ?: return@LaunchedEffect
        if (profileUnlock.unlock(context, listOf(profile.name))) {
            profileManager.markProfileAuthenticated(profile.id)
            open(profile)
        }
        unlocking = null
    }
    val relocked = opened && held && activeProfile?.let { profileManager.profileRequiresUnlock(it.id) } == true
    LaunchedEffect(relocked) {
        if (relocked) unlocking = activeProfile
    }
    LaunchedEffect(opened, gateProfiles, activeProfile?.id) {
        if (opened || unlocking != null || gateProfiles.size != 1) return@LaunchedEffect
        val profile = activeProfile ?: gateProfiles.first()
        val decision = resolvePickerCollapseStartupGateDecision(
            profile = profile,
            requiresProfileUnlock = lock.requiresUnlock(profile.id),
            shouldSkipProfileAuth = false,
            canAuthenticate = canAuthenticate,
        )
        when (decision.state) {
            ProfileStartupGateState.Ready -> open(profile)
            ProfileStartupGateState.Authenticating -> unlocking = decision.pendingAuthProfile
            ProfileStartupGateState.Loading, ProfileStartupGateState.Picker -> Unit
        }
    }

    val picker: @Composable () -> Unit = {
        ScreenMessagesHost {
            Box {
                ProfileGateContent(
                    state = ProfileStartupGateState.Picker,
                    profiles = gateProfiles,
                    activeProfileId = activeProfile?.id,
                    isLocked = { profileManager.profileRequiresUnlock(it.id) },
                    authProfileName = null,
                    onProfileSelected = { profile ->
                        when {
                            !lock.requiresUnlock(profile.id) -> scope.launch { open(profile) }
                            // Without a sign-in to check, the unlock tells why the profile stays shut
                            !canAuthenticate -> scope.launch { profileUnlock.unlock(context, listOf(profile.name)) }
                            else -> unlocking = profile
                        }
                    },
                )
                ScreenMessagesOverlay()
            }
        }
    }
    if (opened) {
        content()
        if (relocked) LockScreen(picker)
    } else {
        picker()
    }
}
