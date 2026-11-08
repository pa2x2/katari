package app.katari.appupdate.mpvaddon.ui

import androidx.compose.runtime.Composable
import app.katari.appupdate.mpvaddon.MpvAddonRelease
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The add-on in the notes of Katari's update that brings it along, headed as the notes' versions are. */
@Composable
internal fun MpvAddonBroughtAlongNotes(addon: MpvAddonRelease) {
    Text(
        text = stringResource(MR.strings.mpv_addon_version, addon.version),
        role = TextRole.ScreenTitle,
        tone = ContentTone.Primary,
    )
    Text(
        text = listOfNotNull(addon.components, stringResource(MR.strings.mpv_addon_update_with_app)).joinToString("\n"),
        role = TextRole.Body,
    )
}
