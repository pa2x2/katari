package mihon.feature.appupdate.mpvaddon.ui

import androidx.compose.runtime.Composable
import mihon.feature.appupdate.mpvaddon.MpvAddonRelease
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.TextRole
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
