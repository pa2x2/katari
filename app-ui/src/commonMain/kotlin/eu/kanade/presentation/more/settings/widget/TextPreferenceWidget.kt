package eu.kanade.presentation.more.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.theme.ContentTone

@Composable
fun TextPreferenceWidget(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleTone: ContentTone = ContentTone.Inherit,
    icon: ImageVector? = null,
    iconAlert: Boolean = false,
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
        iconAlert = iconAlert,
        trailing = widget,
        titleSuffix = settingTitleSuffix(isProfileSpecific),
        subtitleMaxLines = SUBTITLE_MAX_LINES,
        subtitleTone = subtitleTone,
        enabled = enabled,
        onClick = onPreferenceClick,
    )
}

private const val SUBTITLE_MAX_LINES = 10
