package app.katari.presentation.kit.control

import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import app.katari.presentation.kit.theme.KatariTheme

/**
 * A standalone switch. Settings use the kit's toggle row, which picks between a switch and a checkbox. Disabled and on,
 * it keeps a dimmed accent track, where Material's default reads as off in a dark scheme.
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = KatariTheme.colors
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            disabledCheckedTrackColor = colors.disabledSelectedContainer,
            disabledCheckedThumbColor = colors.onPrimary,
        ),
    )
}

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    androidx.compose.material3.Checkbox(checked, onCheckedChange, modifier, enabled)
}

@Composable
fun TriStateCheckbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    androidx.compose.material3.TriStateCheckbox(state, onClick, modifier, enabled)
}

@Composable
fun RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    androidx.compose.material3.RadioButton(selected, onClick, modifier, enabled)
}
