package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.presentation.track.components.TrackLogoIcon
import mihon.entry.interactions.tracking.EntryTrackingServiceDescriptor
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TrackingPreferenceWidget(
    service: EntryTrackingServiceDescriptor,
    isLoggedIn: Boolean,
    displayUsername: String,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    SettingsRow(
        title = service.name,
        modifier = modifier.preferenceHighlight(),
        subtitle = displayUsername.takeIf { isLoggedIn && it.isNotBlank() },
        subtitleMaxLines = 1,
        leading = { TrackLogoIcon(name = service.name, logoResource = service.logoResource) },
        trailing = if (isLoggedIn) {
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
                ) {
                    if (onRefresh != null) {
                        if (isRefreshing) {
                            Box(
                                modifier = Modifier.size(KatariTheme.sizes.touchTarget),
                                contentAlignment = Alignment.Center,
                            ) {
                                LoadingIndicator(modifier = Modifier.size(KatariTheme.sizes.icon))
                            }
                        } else {
                            IconButton(
                                icon = MaterialSymbols.Rounded.Refresh,
                                contentDescription = stringResource(MR.strings.refresh_tracker_profile),
                                onClick = onRefresh,
                            )
                        }
                    }
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Check,
                        contentDescription = stringResource(MR.strings.login_success),
                        tone = ContentTone.Success,
                    )
                }
            }
        } else {
            null
        },
        onClick = onClick,
    )
}
