package mihon.presentation.kit.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowRight
import mihon.icons.materialsymbols.rounded.RadioButtonChecked
import mihon.icons.materialsymbols.rounded.RadioButtonUnchecked
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.Checkbox
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import androidx.compose.material3.DropdownMenu as MaterialDropdownMenu
import androidx.compose.material3.DropdownMenuItem as MaterialDropdownMenuItem

/**
 * Every app menu has the same fixed width, so menus opened from neighbouring buttons line up. [wide] gives room to
 * menus whose items carry their own actions, such as saved presets that can each be edited and deleted.
 */
@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    focusable: Boolean = true,
    wide: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val width = if (wide) WideMenuWidth else MenuWidth
    MaterialDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.sizeIn(minWidth = width, maxWidth = width),
        offset = offset,
        properties = PopupProperties(focusable = focusable),
        content = content,
    )
}

/**
 * [trailing] holds a control or short label at the item's end, such as a delete button or "Retry". [supportingLabel]
 * adds a second line that tells similar items apart, such as where a feed comes from; [leading] replaces [icon] with an
 * image of the item itself, such as a source's icon. [selected] fills the item that is current, such as the open tab.
 */
@Composable
fun DropdownMenuItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    supportingLabel: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
) {
    MaterialDropdownMenuItem(
        text = {
            if (supportingLabel == null) {
                Text(label)
            } else {
                Column {
                    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        supportingLabel,
                        role = TextRole.Supporting,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        onClick = onClick,
        modifier = modifier.then(
            if (selected) Modifier.background(KatariTheme.colors.selected, KatariTheme.shapes.medium) else Modifier,
        ),
        leadingIcon = leading ?: icon?.let { { Icon(imageVector = it, contentDescription = null) } },
        trailingIcon = trailing,
        enabled = enabled,
    )
}

@Composable
fun CheckMenuItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenuItem(
        label = label,
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        trailing = { Checkbox(checked = checked, onCheckedChange = null) },
    )
}

@Composable
fun RadioMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenuItem(
        label = label,
        onClick = onClick,
        modifier = modifier,
        trailing = {
            if (selected) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.RadioButtonChecked,
                    contentDescription = stringResource(MR.strings.selected),
                    tone = ContentTone.Primary,
                )
            } else {
                Icon(
                    imageVector = MaterialSymbols.Rounded.RadioButtonUnchecked,
                    contentDescription = stringResource(MR.strings.not_selected),
                )
            }
        },
    )
}

/** An item that opens a submenu; [children] receives a callback that closes the submenu. */
@Composable
fun NestedMenuItem(
    label: String,
    modifier: Modifier = Modifier,
    children: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val close = { expanded = false }
    Box {
        DropdownMenuItem(
            label = label,
            onClick = { expanded = true },
            trailing = {
                Icon(imageVector = MaterialSymbols.AutoMirroredRounded.ArrowRight, contentDescription = null)
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = close, modifier = modifier) {
            children(close)
        }
    }
}

private val MenuWidth = 196.dp
private val WideMenuWidth = 280.dp
