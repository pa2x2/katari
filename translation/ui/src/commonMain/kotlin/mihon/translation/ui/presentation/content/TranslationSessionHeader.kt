package mihon.translation.ui.presentation.content

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.OpenInNew
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.CloseFullscreen
import mihon.icons.materialsymbols.rounded.OpenInFull
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The title row of the translation popup and sheet. The popup's Close ends the translation; the sheet closes like any
 * other sheet, and its collapse returns fullscreen to the popup it was opened from.
 */
@Composable
internal fun TranslationSessionHeader(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onExpand: (() -> Unit)? = null,
    onCollapse: (() -> Unit)? = null,
    onDocumentation: (() -> Unit)? = null,
) {
    if (!compact) {
        SheetHeader(
            title = title,
            modifier = modifier,
            actions = onCollapse?.let { collapse ->
                {
                    IconButton(
                        icon = MaterialSymbols.Rounded.CloseFullscreen,
                        contentDescription = stringResource(MR.strings.action_collapse),
                        onClick = collapse,
                    )
                }
            },
        )
        return
    }
    val spacing = KatariTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = spacing.l, end = spacing.xs, top = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, modifier = Modifier.weight(1f), role = TextRole.SectionTitle)
        onDocumentation?.let {
            IconButton(
                icon = MaterialSymbols.AutoMirroredRounded.OpenInNew,
                contentDescription = stringResource(MR.strings.action_learn_more),
                onClick = it,
            )
        }
        onExpand?.let {
            IconButton(
                icon = MaterialSymbols.Rounded.OpenInFull,
                contentDescription = stringResource(MR.strings.action_expand),
                onClick = it,
            )
        }
        IconButton(
            icon = MaterialSymbols.Rounded.Close,
            contentDescription = stringResource(MR.strings.action_close),
            onClick = onDismiss,
        )
    }
}
