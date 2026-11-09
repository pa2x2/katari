package app.katari.sync.ui.choice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.katari.core.platform.currentPlatformContext
import app.katari.profiles.core.ProfileConstants
import app.katari.profiles.ui.switcher.switchToProfile
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import tachiyomi.i18n.*

/**
 * What this device syncs: opened from the Sync page's options, its set-up rows, the first-sync choices and settings
 * search. Each area's own list of settings opens in [SettingsAreaSyncScreen].
 */
class WhatSyncsScreen : SearchableSettings() {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.sync_choices_title

    @Composable
    override fun getPreferences(): List<Preference> {
        val navigator = LocalNavigator.currentOrThrow
        val model = metroViewModel<WhatSyncsViewModel>()
        val choices by model.choices.collectAsState()
        val profiles by model.profiles.collectAsState()
        return whatSyncsPreferences(
            choices = choices,
            profiles = profiles,
            askBeforeNewProfiles = model.preferences.askBeforeNewProfiles,
            actions = WhatSyncsActions(
                syncData = model::syncData,
                syncArea = model::syncArea,
                openArea = { navigator.push(SettingsAreaSyncScreen(it)) },
                syncProfile = model::syncProfile,
            ),
        )
    }

    @Composable
    override fun Content() {
        super.Content()
        val model = metroViewModel<WhatSyncsViewModel>()
        AreaAgainQuestion(model)
        ProfileRemovalQuestion(model)
    }
}

/** Asks whose settings an area turned back on keeps, while the model waits for the answer. */
@Composable
internal fun AreaAgainQuestion(model: WhatSyncsViewModel) {
    val area by model.askingArea.collectAsState()
    area?.let {
        AreaAgainDialog(area = it, onAnswer = model::answerArea, onDismissRequest = model::dismissAreaQuestion)
    }
}

/**
 * Confirms that a profile switched off leaves this device. Where it is the open profile, Katari switches to the default
 * profile, which every device syncs, before it goes.
 */
@Composable
private fun ProfileRemovalQuestion(model: WhatSyncsViewModel) {
    val profile by model.removingProfile.collectAsState()
    val removing = profile ?: return
    val context = currentPlatformContext()
    val graph = LocalAppUiGraph.current
    val profileManager = graph.profileManager
    val active by profileManager.activeProfile.collectAsState()
    val profiles by profileManager.profiles.collectAsState()
    val switchTo = profiles.firstOrNull { it.id == ProfileConstants.DEFAULT_PROFILE_ID }
        ?.takeIf { active?.uuid == removing.uuid }
    RemoveProfileDialog(
        profile = removing,
        switchTo = switchTo,
        onConfirm = {
            model.removeProfile(
                switchAway = switchTo?.let { target ->
                    {
                        switchToProfile(
                            context = context,
                            profileManager = profileManager,
                            profileUnlock = graph.profileUnlock,
                            switchEffects = graph.profileSwitchEffects,
                            profile = target,
                        )
                    }
                },
            )
        },
        onDismissRequest = model::dismissProfileRemoval,
    )
}
