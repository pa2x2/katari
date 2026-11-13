package app.katari.sync.ui.choice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import app.katari.sync.client.part.ChoiceSet
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
    override fun getPreferences(): List<Preference> = preferences(withChosenApart = true)

    // The settings chosen one by one are found under their own screens
    @Composable
    override fun getSearchedPreferences(): List<Preference> = preferences(withChosenApart = false)

    @Composable
    private fun preferences(withChosenApart: Boolean): List<Preference> {
        val navigator = LocalNavigator.currentOrThrow
        val model = metroViewModel<WhatSyncsViewModel>()
        val choices by model.choices.collectAsState()
        val profiles by model.profiles.collectAsState()
        val actions = WhatSyncsActions(
            syncData = model::syncData,
            syncArea = model::syncArea,
            openArea = { navigator.push(SettingsAreaSyncScreen(it)) },
            syncProfile = model::syncProfile,
            syncSetting = model::syncSetting,
            syncMenu = model::syncMenu,
        )
        return whatSyncsPreferences(
            choices = choices,
            chosenApart = if (withChosenApart) chosenApartPreferences(choices, actions) else null,
            profiles = profiles,
            askBeforeNewProfiles = model.preferences.askBeforeNewProfiles,
            actions = actions,
        )
    }

    @Composable
    private fun chosenApartPreferences(choices: ChoiceSet, actions: WhatSyncsActions): Preference.PreferenceGroup? {
        // What was chosen apart when the page opened stays listed while it is open, so a row switched back can be
        // switched again
        val opened = rememberSaveable(saver = ChosenApart.Saver) { ChosenApart.of(choices) }
        return chosenApartGroup(choices, opened + ChosenApart.of(choices), settingTitles(), actions)
    }

    @Composable
    override fun Content() {
        super.Content()
        val model = metroViewModel<WhatSyncsViewModel>()
        SettingsAgainQuestion(model)
        ProfileRemovalQuestion(model)
    }
}
