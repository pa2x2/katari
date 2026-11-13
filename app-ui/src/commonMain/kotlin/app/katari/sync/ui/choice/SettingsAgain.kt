package app.katari.sync.ui.choice

import androidx.compose.runtime.Composable
import app.katari.sync.client.firstsync.SettingsWinner
import app.katari.sync.client.setting.scope.MenuSettings
import app.katari.sync.client.setting.scope.SettingsArea
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Settings being synced again that this device changed while it didn't sync them, which wait for the person to say
 * whose values they keep: the account's, or this device's.
 */
internal sealed interface SettingsAgain {
    data class Area(val area: SettingsArea) : SettingsAgain

    /** One setting, which the question names by [title], its row's title. */
    data class Setting(val key: String, val title: String) : SettingsAgain

    data class Menu(val menu: MenuSettings) : SettingsAgain
}

@Composable
internal fun SettingsAgain.questionTitle(): String = when (this) {
    is SettingsAgain.Area -> stringResource(MR.strings.sync_area_again_title, area.title())
    is SettingsAgain.Setting -> stringResource(MR.strings.sync_setting_again_title, title)
    is SettingsAgain.Menu -> stringResource(MR.strings.sync_setting_again_title, menu.title())
}

@Composable
internal fun SettingsAgain.questionText(): String = stringResource(
    when (this) {
        is SettingsAgain.Setting -> MR.strings.sync_setting_again_text
        is SettingsAgain.Area, is SettingsAgain.Menu -> MR.strings.sync_area_again_text
    },
)

/** How [SettingsAgain]'s question names keeping [winner]'s values. */
@Composable
internal fun SettingsAgain.answer(winner: SettingsWinner): String {
    val one = this is SettingsAgain.Setting
    return stringResource(
        when (winner) {
            SettingsWinner.ACCOUNT ->
                if (one) MR.strings.sync_setting_keep_account else MR.strings.sync_area_keep_account
            SettingsWinner.DEVICE ->
                if (one) MR.strings.sync_setting_keep_device else MR.strings.sync_area_keep_device
        },
    )
}
