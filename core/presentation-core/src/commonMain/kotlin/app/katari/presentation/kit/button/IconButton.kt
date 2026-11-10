package app.katari.presentation.kit.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Spin
import app.katari.presentation.kit.content.SpinningIcon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.feedback.HoverTooltip
import app.katari.presentation.kit.shortcut.KeyboardShortcut
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.LocalContentColor
import app.katari.presentation.kit.theme.color

/** Emphasis of an icon button; [Standard] is a bare icon, as in app bars and rows. */
enum class IconButtonStyle { Standard, Filled, Tonal, Outlined }

/** Touch target of an icon button; [Large] is for controls over full-screen media. The icon stays the same size. */
enum class IconButtonSize { Standard, Large }

/**
 * [spin] turns the icon for as long as the work it stands for runs, the way the icon's arrows point. With [arrows],
 * drawn over [icon], only they turn, such as sync's arrows inside its still cloud.
 *
 * Disabled, the icon dims whatever its [tone], such as Previous and Next over a video while another episode opens.
 *
 * In a window worked with a mouse, hovering the button shows [contentDescription] as its tooltip, with the keys of
 * [shortcut], the shortcut that does what the button does, unless the button already sits in a tooltip, such as an app
 * bar action's.
 */
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
    shortcut: KeyboardShortcut? = null,
    spin: Spin? = null,
    arrows: ImageVector? = null,
) {
    val sizedModifier = when (size) {
        IconButtonSize.Standard -> modifier
        IconButtonSize.Large -> modifier.size(KatariTheme.sizes.touchTargetLarge)
    }
    // Squares off while pressed
    val shapes = IconButtonDefaults.shapes()
    val toneDimmed = !enabled && tone != ContentTone.Inherit && tone != ContentTone.Original
    val content: @Composable () -> Unit = {
        // A tone replaces the content colour the button dims when disabled, so a disabled toned icon dims its tone.
        val iconTone = if (toneDimmed) ContentTone.Inherit else tone
        val toned: @Composable () -> Unit = {
            if (badgeCount != null && badgeCount > 0) {
                BadgedBox(badge = { Badge { Text(badgeCount.toString()) } }) {
                    SpinningIcon(icon, contentDescription, spin, arrows, iconTone)
                }
            } else {
                SpinningIcon(icon, contentDescription, spin, arrows, iconTone)
            }
        }
        if (toneDimmed) {
            CompositionLocalProvider(LocalContentColor provides KatariTheme.colors.disabled(tone.color()), toned)
        } else {
            toned()
        }
    }
    HoverTooltip(contentDescription, shortcut = shortcut) {
        when (style) {
            IconButtonStyle.Standard ->
                androidx.compose.material3.IconButton(onClick, shapes, sizedModifier, enabled, content = content)
            IconButtonStyle.Filled -> FilledIconButton(onClick, shapes, sizedModifier, enabled, content = content)
            IconButtonStyle.Tonal -> FilledTonalIconButton(onClick, shapes, sizedModifier, enabled, content = content)
            IconButtonStyle.Outlined -> OutlinedIconButton(onClick, shapes, sizedModifier, enabled, content = content)
        }
    }
}

/**
 * An icon button that stays on while [checked]; [checkedIcon] replaces [icon] when it does. It shows a tooltip on
 * hover as [IconButton] does.
 */
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
    HoverTooltip(contentDescription) {
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
}
