package app.katari.presentation.kit.menu

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.ArrowDropDown
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.surface.Surface
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.theme.KatariTheme

/**
 * The current choice of a menu, on a pill that opens the menu, such as the selected feed. [leading] marks the choice,
 * such as with its logo, and [label] names it. [onLongClick] is a second action on the choice, which
 * [onLongClickLabel] describes to accessibility services.
 */
@Composable
fun MenuPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    leading: (@Composable () -> Unit)? = null,
    label: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .heightIn(min = KatariTheme.sizes.touchTarget)
            .clip(KatariTheme.shapes.full)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = onLongClickLabel),
        container = SurfaceContainer.SecondaryContainer,
        shape = KatariTheme.shapes.full,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            label()
            Icon(imageVector = MaterialSymbols.Rounded.ArrowDropDown, contentDescription = null)
        }
    }
}
