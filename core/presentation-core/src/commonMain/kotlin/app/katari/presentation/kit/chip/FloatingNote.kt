package app.katari.presentation.kit.chip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.feedback.LocalSnackbarShowing
import app.katari.presentation.kit.surface.Surface
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole

/**
 * A short note floating beside the screen's floating action about what it does right now, such as where the progress
 * Resume continues came from. It is read, not pressed; [icon] names what it is about, such as a device. It steps aside
 * while the scaffold shows a snackbar above the action, so one note at a time floats there.
 */
@Composable
fun FloatingNote(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val snackbarShowing = LocalSnackbarShowing.current
    AnimatedVisibility(visible = !snackbarShowing(), modifier = modifier) {
        NoteSurface(text, icon)
    }
}

@Composable
private fun NoteSurface(text: String, icon: ImageVector?) {
    Surface(
        container = SurfaceContainer.SecondaryContainer,
        shape = KatariTheme.shapes.full,
        shadow = true,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(KatariTheme.sizes.iconSmall)) }
            Text(text = text, role = TextRole.Label)
        }
    }
}
