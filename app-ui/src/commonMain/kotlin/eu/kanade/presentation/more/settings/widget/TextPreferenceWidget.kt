package eu.kanade.presentation.more.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import mihon.presentation.kit.list.SettingsRow

@Composable
fun TextPreferenceWidget(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    isProfileSpecific: Boolean = false,
    widget: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    onPreferenceClick: (() -> Unit)? = null,
) {
    SettingsRow(
        title = title,
        modifier = modifier.preferenceHighlight(),
        subtitle = subtitle,
        icon = icon,
        trailing = widget,
        titleSuffix = profileSpecificSuffix(isProfileSpecific),
        subtitleMaxLines = SUBTITLE_MAX_LINES,
        enabled = enabled,
        onClick = onPreferenceClick,
    )
}

private const val SUBTITLE_MAX_LINES = 10
