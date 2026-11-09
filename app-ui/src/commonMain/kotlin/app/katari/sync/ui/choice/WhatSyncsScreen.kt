package app.katari.sync.ui.choice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
