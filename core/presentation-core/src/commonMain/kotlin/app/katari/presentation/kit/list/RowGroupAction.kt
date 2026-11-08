package app.katari.presentation.kit.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.theme.KatariTheme

/**
 * Another way on, set between row group cards as a text button lined up with their edge, such as typing a code instead
 * of scanning it, or opening in a browser the page a [RowGroupNote] above it explains.
 */
@Composable
fun RowGroupAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Box(modifier.padding(horizontal = KatariTheme.spacing.m)) {
        Button(label = label, onClick = onClick, style = ButtonStyle.Text, icon = icon)
    }
}
