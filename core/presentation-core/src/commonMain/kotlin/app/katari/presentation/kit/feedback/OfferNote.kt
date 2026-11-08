package app.katari.presentation.kit.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Close
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonSize
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.surface.Surface
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * An offer floating over what is being read or watched, such as going to where another device got to. Unlike a
 * message it doesn't time out: it stays until [onAction] or [onDismiss], or until what it offers stops applying, which
 * its owner decides, such as when a page is turned. [icon] names what it is about.
 */
@Composable
fun OfferNote(
    text: String,
    actionLabel: String,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Surface(
        modifier = modifier.widthIn(max = OfferMaxWidth),
        container = SurfaceContainer.SecondaryContainer,
        shape = KatariTheme.shapes.medium,
        shadow = true,
    ) {
        Row(
            modifier = Modifier.padding(start = KatariTheme.spacing.l, end = KatariTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(KatariTheme.sizes.iconSmall)) }
            Text(
                text = text,
                role = TextRole.Body,
                modifier = Modifier.weight(1f).padding(vertical = KatariTheme.spacing.m),
            )
            Button(label = actionLabel, onClick = onAction, style = ButtonStyle.Text, size = ButtonSize.Small)
            IconButton(
                icon = MaterialSymbols.Rounded.Close,
                contentDescription = stringResource(MR.strings.action_close),
                onClick = onDismiss,
            )
        }
    }
}

// As wide as a snackbar grows, so the offer reads as one line on a tablet rather than spanning the page
private val OfferMaxWidth = 560.dp
