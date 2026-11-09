package app.katari.sync.ui.choice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.katari.sync.client.setting.scope.SettingsArea
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.more.settings.PreferenceScaffold
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen

/**
 * One Settings area's settings and whether each syncs: its switch turns the whole area on and off, and each setting
 * the area syncs can be kept out of it, or, for one that stays on each device by default, brought in.
 */
class SettingsAreaSyncScreen(private val area: SettingsArea) : Screen() {

    @Composable
    override fun Content() {
        val backPress = LocalBackPress.current
        val model = metroViewModel<WhatSyncsViewModel>()
        val choices by model.choices.collectAsState()
        PreferenceScaffold(
            titleRes = area.titleRes,
            onBackPressed = backPress,
            itemsProvider = {
                areaSyncPreferences(
                    area = area,
                    choices = choices,
                    settings = areaSettings(area),
                    onSyncArea = { model.syncArea(area, it) },
                    onSyncSetting = model::syncSetting,
                )
            },
        )
        AreaAgainQuestion(model)
    }
}
