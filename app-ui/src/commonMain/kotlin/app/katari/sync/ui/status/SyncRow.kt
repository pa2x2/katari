package app.katari.sync.ui.status

import androidx.compose.runtime.Composable
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.CloudSync
import app.katari.presentation.kit.theme.ContentTone
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The Sync row in More (D028), always shown: on a standalone device it invites setting sync up, and once set up its
 * subtitle is sync's everyday indicator, coloured only for trouble that lasts.
 */
@Composable
fun SyncRow(onClick: () -> Unit) {
    val headline = rememberSyncHeadline()
    TextPreferenceWidget(
        title = stringResource(MR.strings.sync_title),
        subtitle = headline?.let { syncHeadlineText(it) },
        subtitleTone = headline?.tone() ?: ContentTone.Inherit,
        icon = headline?.icon() ?: MaterialSymbols.Rounded.CloudSync,
        onPreferenceClick = onClick,
    )
}
