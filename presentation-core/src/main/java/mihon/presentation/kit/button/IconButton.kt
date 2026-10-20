package mihon.presentation.kit.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme

/** Emphasis of an icon button; [Standard] is a bare icon, as in app bars and rows. */
enum class IconButtonStyle { Standard, Filled, Tonal, Outlined }

/** Touch target of an icon button; [Large] is for controls over full-screen media. The icon stays the same size. */
enum class IconButtonSize { Standard, Large }

@Composable
fun IconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IconButtonStyle = IconButtonStyle.Standard,
    size: IconButtonSize = IconButtonSize.Standard,
    tone: ContentTone = ContentTone.Inherit,
    enabled: Boolean = true,
    badgeCount: Int? = null,
) {
    val sizedModifier = when (size) {
        IconButtonSize.Standard -> modifier
        IconButtonSize.Large -> modifier.size(KatariTheme.sizes.touchTargetLarge)
    }
    // Squares off while pressed
    val shapes = IconButtonDefaults.shapes()
    val content: @Composable () -> Unit = {
        if (badgeCount != null && badgeCount > 0) {
            BadgedBox(badge = { Badge { Text(badgeCount.toString()) } }) {
                Icon(icon, contentDescription, tone = tone)
            }
        } else {
            Icon(icon, contentDescription, tone = tone)
        }
    }
    when (style) {
        IconButtonStyle.Standard ->
            androidx.compose.material3.IconButton(onClick, shapes, sizedModifier, enabled, content = content)
        IconButtonStyle.Filled -> FilledIconButton(onClick, shapes, sizedModifier, enabled, content = content)
        IconButtonStyle.Tonal -> FilledTonalIconButton(onClick, shapes, sizedModifier, enabled, content = content)
        IconButtonStyle.Outlined -> OutlinedIconButton(onClick, shapes, sizedModifier, enabled, content = content)
    }
}

/** An icon button that stays on while [checked]; [checkedIcon] replaces [icon] when it does. */
@Composable
fun IconToggleButton(
    icon: ImageVector,
    contentDescription: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    checkedIcon: ImageVector = icon,
    filled: Boolean = false,
    enabled: Boolean = true,
) {
    val content: @Composable () -> Unit = {
        Icon(if (checked) checkedIcon else icon, contentDescription)
    }
    if (filled) {
        FilledIconToggleButton(checked, onCheckedChange, modifier, enabled, content = content)
    } else {
        androidx.compose.material3.IconToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            colors = IconButtonDefaults.iconToggleButtonColors(),
            content = content,
        )
    }
}
