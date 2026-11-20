package eu.kanade.presentation.more.settings.screen.animeplayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import app.katari.appupdate.mpvaddon.MpvAddonController
import app.katari.appupdate.mpvaddon.MpvAddonUpdatePreferences
import app.katari.appupdate.mpvaddon.restartKatari
import app.katari.video.engine.mpv.addon.MpvAddon
import app.katari.video.engine.mpv.addon.MpvAddonState
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.Preference
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The mpv add-on, which Android installs as an application of its own that the player loads mpv's libraries from. */
@Inject
class MpvPlayerEngineAddon internal constructor(
    private val addon: MpvAddon,
    private val controller: MpvAddonController,
    private val preferences: MpvAddonUpdatePreferences,
) : PlayerEngineAddon {

    @Composable
    override fun settingsItems(): List<Preference.PreferenceItem<out Any, out Any>> {
        val context = LocalContext.current
        val state by addon.state.collectAsState()
        // A missing or outdated add-on already offers what to install on its own row.
        val current = (state as? MpvAddonState.Installed)?.let { it.problem == null } == true
        return listOfNotNull(
            Preference.PreferenceItem.CustomPreference(title = stringResource(MR.strings.mpv_addon)) {
                MpvAddonSettingsRow(
                    addon = addon,
                    installation = controller,
                    onRestart = { restartKatari(context) },
                )
            },
            Preference.PreferenceItem.CustomPreference(title = stringResource(MR.strings.check_for_updates)) {
                MpvAddonUpdateCheckRow(controller, preferences)
            }.takeIf { current },
        )
    }
}
