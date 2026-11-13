package eu.kanade.presentation.more.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.presentation.kit.list.ToggleRow
import eu.kanade.presentation.more.settings.LocalPreferenceSetting

@Composable
fun SwitchPreferenceWidget(
    title: String,
    onCheckedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
    isProfileSpecific: Boolean = false,
    checked: Boolean = false,
    enabled: Boolean = true,
    onDisabledClick: (() -> Unit)? = null,
    onOpen: (() -> Unit)? = null,
) {
    ToggleRow(
        title = title,
        checked = checked,
        onCheckedChange = onCheckedChanged,
        modifier = modifier.preferenceHighlight(),
        subtitle = subtitle,
        icon = icon,
        leading = leading,
        enabled = enabled,
        titleSuffix = settingTitleSuffix(isProfileSpecific),
        setting = LocalPreferenceSetting.current,
        onDisabledClick = onDisabledClick,
        onOpen = onOpen,
    )
}
