package eu.kanade.presentation.more.settings.screen.animeplayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.katari.appupdate.mpvaddon.restartKatari
import app.katari.video.engine.mpv.addon.MpvAddon
import app.katari.video.engine.mpv.install.MpvAddonInstallation
import dev.zacsweers.metro.Inject

/** The mpv add-on, which Android installs as an application of its own that the player loads mpv's libraries from. */
@Inject
class MpvPlayerEngineAddon(
    private val addon: MpvAddon,
    private val installation: MpvAddonInstallation,
) : PlayerEngineAddon {
    override val isOffered: Boolean = true

    @Composable
    override fun SettingsRow() {
        val context = LocalContext.current
        MpvAddonSettingsRow(
            addon = addon,
            installation = installation,
            onRestart = { restartKatari(context) },
        )
    }
}
