package mihon.entry.interactions.source

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.MoreVert
import mihon.icons.materialsymbols.rounded.OpenInBrowser
import mihon.icons.materialsymbols.rounded.Public
import mihon.icons.materialsymbols.rounded.Share
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.button.IconButtonSize
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.theme.ContentTone
import tachiyomi.i18n.*
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
    tone: ContentTone = ContentTone.Inherit,
    size: IconButtonSize = IconButtonSize.Standard,
) {
    if (resolution == null) return

    var expanded by remember(resolution) { mutableStateOf(false) }

    IconButton(
        icon = MaterialSymbols.Rounded.MoreVert,
        contentDescription = stringResource(MR.strings.label_more),
        onClick = { expanded = true },
        size = size,
        tone = tone,
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        EntryChildWebViewActionItems(resolution, onAction, close = { expanded = false })
    }
}

/** The child-WebView actions as items of a menu the host owns, for a host that adds items of its own; [close] closes it. */
@Composable
fun ColumnScope.EntryChildWebViewActionItems(
    resolution: EntryChildWebViewResolution.Available,
    onAction: (EntryChildWebViewAction, EntryChildWebViewResolution.Available) -> Unit,
    close: () -> Unit,
) {
    EntryChildWebViewAction.entries.forEach { action ->
        DropdownMenuItem(
            label = stringResource(
                when (action) {
                    EntryChildWebViewAction.OPEN_IN_WEB_VIEW -> MR.strings.action_open_in_web_view
                    EntryChildWebViewAction.OPEN_IN_BROWSER -> MR.strings.action_open_in_browser
                    EntryChildWebViewAction.SHARE -> MR.strings.action_share
                },
            ),
            onClick = {
                close()
                onAction(action, resolution)
            },
            icon = when (action) {
                EntryChildWebViewAction.OPEN_IN_WEB_VIEW -> MaterialSymbols.Rounded.Public
                EntryChildWebViewAction.OPEN_IN_BROWSER -> MaterialSymbols.Rounded.OpenInBrowser
                EntryChildWebViewAction.SHARE -> MaterialSymbols.Rounded.Share
            },
        )
    }
}
