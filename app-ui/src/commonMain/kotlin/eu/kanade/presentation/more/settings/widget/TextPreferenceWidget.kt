package eu.kanade.presentation.more.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.presentation.kit.content.TonedSpan
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.theme.ContentTone
import eu.kanade.presentation.more.settings.LocalPreferenceSetting

@Composable
fun TextPreferenceWidget(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleTone: ContentTone = ContentTone.Inherit,
    subtitleState: TonedSpan? = null,
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
        setting = LocalPreferenceSetting.current,
        subtitleMaxLines = SUBTITLE_MAX_LINES,
        subtitleTone = subtitleTone,
        subtitleState = subtitleState,
        enabled = enabled,
        onClick = onPreferenceClick,
    )
}

private const val SUBTITLE_MAX_LINES = 10
