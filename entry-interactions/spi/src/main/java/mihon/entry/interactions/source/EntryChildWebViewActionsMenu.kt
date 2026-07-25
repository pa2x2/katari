package mihon.entry.interactions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

enum class EntryChildWebViewAction {
    OPEN_IN_WEB_VIEW,
    OPEN_IN_BROWSER,
    SHARE,
}

/**
 * Shared child-WebView actions gated by an available [EntryWebViewFeature] result.
 *
 * Reader and player hosts must not infer availability from source types or child URLs. Passing the
 * Feature resolution keeps visibility and execution behind the canonical Feature relationship.
 */
@Composable
fun EntryChildWebViewActionsMenu(
    resolution: EntryChildWebViewResolution.Available?,
    onAction: (EntryChildWebViewAction, EntryChildWebViewResolution.Available) -> Unit,
    contentColor: Color = LocalContentColor.current,
) {
    if (resolution == null) return

    var expanded by remember(resolution) { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = stringResource(MR.strings.label_more),
            tint = contentColor,
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        EntryChildWebViewAction.entries.forEach { action ->
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            when (action) {
                                EntryChildWebViewAction.OPEN_IN_WEB_VIEW -> MR.strings.action_open_in_web_view
                                EntryChildWebViewAction.OPEN_IN_BROWSER -> MR.strings.action_open_in_browser
                                EntryChildWebViewAction.SHARE -> MR.strings.action_share
                            },
                        ),
                    )
                },
                onClick = {
                    expanded = false
                    onAction(action, resolution)
                },
            )
        }
    }
}
