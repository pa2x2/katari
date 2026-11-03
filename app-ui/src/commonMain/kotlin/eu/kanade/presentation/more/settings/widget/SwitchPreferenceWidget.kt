package eu.kanade.presentation.more.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import mihon.presentation.kit.list.ToggleRow

@Composable
fun SwitchPreferenceWidget(
    title: String,
    onCheckedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    isProfileSpecific: Boolean = false,
    checked: Boolean = false,
    enabled: Boolean = true,
    onDisabledClick: (() -> Unit)? = null,
) {
    ToggleRow(
        title = title,
        checked = checked,
        onCheckedChange = onCheckedChanged,
        modifier = modifier.preferenceHighlight(),
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        titleSuffix = profileSpecificSuffix(isProfileSpecific),
        onDisabledClick = onDisabledClick,
    )
}
