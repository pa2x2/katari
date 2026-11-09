package app.katari.sync.ui.choice

import androidx.compose.runtime.Composable
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.CloudSync
import app.katari.presentation.kit.button.IconButton
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A Settings screen's app bar action opening the sync page of the area its [preferences] belong to, while this device
 * is linked to an account.
 */
@Composable
fun SettingsAreaSyncAction(preferences: List<Preference>) {
    if (rememberSettingSyncMarks() == null) return
    val area = preferences.syncArea()?.takeIf { it.hasPage } ?: return
    val navigator = LocalNavigator.currentOrThrow
    IconButton(
        icon = MaterialSymbols.Rounded.CloudSync,
        contentDescription = stringResource(MR.strings.sync_choices_title),
        onClick = { navigator.push(SettingsAreaSyncScreen(area)) },
    )
}
