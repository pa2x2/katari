package mihon.presentation.kit.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CheckBoxOutlineBlank
import mihon.icons.materialsymbols.roundedfilled.CheckBox
import mihon.icons.materialsymbols.roundedfilled.DisabledByDefault
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.control.Checkbox
import mihon.presentation.kit.control.RadioButton
import mihon.presentation.kit.control.Switch
import mihon.presentation.kit.theme.ContentTone
import tachiyomi.core.common.preference.TriState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * An on/off setting. The row's density picks the control: a trailing switch on screens, a leading checkbox in sheets.
 * The checkbox takes the place of [icon], so a compact row shows no icon. [onDisabledClick] runs when the row is tapped
 * while disabled, such as to explain or fix why.
 */
@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    titleSuffix: (@Composable () -> Unit)? = null,
    onDisabledClick: (() -> Unit)? = null,
) {
    val compact = LocalRowDensity.current == RowDensity.Compact
    SettingsRow(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        icon = icon,
        leading = if (compact) {
            { Checkbox(checked, onCheckedChange = null, enabled = enabled) }
        } else {
            null
        },
        trailing = if (compact) {
            null
        } else {
            { Switch(checked, onCheckedChange = null, enabled = enabled) }
        },
        titleSuffix = titleSuffix,
        enabled = enabled || onDisabledClick != null,
        role = if (compact) Role.Checkbox else Role.Switch,
        onClick = if (enabled) {
            { onCheckedChange(!checked) }
        } else {
            onDisabledClick
        },
    )
}

/**
 * One option of a set where only one can be chosen. [below] holds what the option shows under its text, such as the
 * state of what it needs and an action for it.
 */
@Composable
fun ChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    below: (@Composable () -> Unit)? = null,
) {
    SettingsRow(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        leading = { RadioButton(selected, onClick = null, enabled = enabled) },
        trailing = trailing,
        enabled = enabled,
        role = Role.RadioButton,
        onClick = onClick,
        below = below,
    )
}

/**
 * One item of a set where any number can be picked, such as filters or categories. [avatar] follows the checkbox when
 * the item is something with one, such as a profile.
 */
@Composable
fun CheckRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    avatar: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    SettingsRow(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        leading = {
            Checkbox(checked, onCheckedChange = null, enabled = enabled)
            avatar?.invoke()
        },
        trailing = trailing,
        enabled = enabled,
        role = Role.Checkbox,
        onClick = { onCheckedChange(!checked) },
    )
}

/** A filter that can include, exclude or ignore what it names; each tap moves to the next state. */
@Composable
fun TriStateRow(
    title: String,
    state: TriState,
    onStateChange: ((TriState) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val stateText = stringResource(
        when (state) {
            TriState.DISABLED -> MR.strings.tristate_not_used
            TriState.ENABLED_IS -> MR.strings.tristate_included
            TriState.ENABLED_NOT -> MR.strings.tristate_excluded
        },
    )
    val interactive = enabled && onStateChange != null
    SettingsRow(
        title = title,
        modifier = modifier.semantics { stateDescription = stateText },
        // An X box rather than Material's dash, so "excluded" does not read as "partly selected".
        leading = {
            Icon(
                imageVector = when (state) {
                    TriState.DISABLED -> MaterialSymbols.Rounded.CheckBoxOutlineBlank
                    TriState.ENABLED_IS -> MaterialSymbols.RoundedFilled.CheckBox
                    TriState.ENABLED_NOT -> MaterialSymbols.RoundedFilled.DisabledByDefault
                },
                contentDescription = null,
                tone = when {
                    !interactive -> ContentTone.Disabled
                    state == TriState.DISABLED -> ContentTone.Secondary
                    else -> ContentTone.Primary
                },
            )
        },
        enabled = interactive,
        role = Role.Checkbox,
        onClick = {
            onStateChange?.invoke(
                when (state) {
                    TriState.DISABLED -> TriState.ENABLED_IS
                    TriState.ENABLED_IS -> TriState.ENABLED_NOT
                    TriState.ENABLED_NOT -> TriState.DISABLED
                },
            )
        },
    )
}
