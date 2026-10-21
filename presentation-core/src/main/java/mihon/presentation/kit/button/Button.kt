package mihon.presentation.kit.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.KatariColors
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.presentation.core.components.material.ButtonColors
import tachiyomi.presentation.core.components.material.Button as ForkedButton
import tachiyomi.presentation.core.components.material.ButtonDefaults as ForkedButtonDefaults

/** Emphasis of a button, from the screen's main action ([Filled]) to a low-priority one ([Text]). */
enum class ButtonStyle { Filled, Tonal, Outlined, Text }

/**
 * The app's button. [destructive] marks an action that deletes or discards something; a destructive [Filled]
 * button is the confirm style of destructive dialogs. [loading] shows the wait indicator in place of [icon], for an
 * action that is already running.
 */
@Composable
fun Button(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Filled,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    loading: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = KatariTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    ForkedButton(
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        enabled = enabled,
        interactionSource = interactionSource,
        elevation = if (style == ButtonStyle.Filled) ForkedButtonDefaults.buttonElevation() else null,
        shape = pressMorphShape(interactionSource),
        border = if (style == ButtonStyle.Outlined) {
            BorderStroke(
                KatariTheme.sizes.border,
                when {
                    !enabled -> colors.disabledContainer
                    destructive -> colors.error
                    else -> colors.outline
                },
            )
        } else {
            null
        },
        colors = buttonColors(style, destructive, colors),
        contentPadding = contentPadding(style, icon != null || loading),
    ) {
        if (loading) {
            LoadingIndicator(modifier = Modifier.size(KatariTheme.sizes.iconSmall))
            Spacer(Modifier.width(KatariTheme.spacing.s))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(KatariTheme.sizes.iconSmall))
            Spacer(Modifier.width(KatariTheme.spacing.s))
        }
        Text(label)
    }
}

private fun buttonColors(style: ButtonStyle, destructive: Boolean, colors: KatariColors): ButtonColors {
    val (container, content) = when (style) {
        ButtonStyle.Filled -> if (destructive) colors.error to colors.onError else colors.primary to colors.onPrimary
        ButtonStyle.Tonal -> if (destructive) {
            colors.errorContainer to colors.onErrorContainer
        } else {
            colors.secondaryContainer to colors.onSecondaryContainer
        }
        ButtonStyle.Outlined, ButtonStyle.Text -> Color.Transparent to if (destructive) colors.error else colors.primary
    }
    return ButtonColors(
        containerColor = container,
        contentColor = content,
        disabledContainerColor = if (container == Color.Transparent) Color.Transparent else colors.disabledContainer,
        disabledContentColor = colors.disabledContent,
    )
}

private fun contentPadding(style: ButtonStyle, hasIcon: Boolean): PaddingValues = when {
    style == ButtonStyle.Text -> ButtonDefaults.TextButtonContentPadding
    hasIcon -> ButtonDefaults.ButtonWithIconContentPadding
    else -> ButtonDefaults.ContentPadding
}
