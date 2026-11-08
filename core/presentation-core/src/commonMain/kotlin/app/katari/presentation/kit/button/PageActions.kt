package app.katari.presentation.kit.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.presentation.kit.theme.KatariTheme

/** A way on from a page, for [PageActions]. */
data class PageAction(val label: String, val onClick: () -> Unit, val icon: ImageVector? = null)

/**
 * The ways on from a page that explains something before the person starts, such as setting up a feature, stacked
 * under its row group cards and as wide as they are: [main] filled, each of [others] outlined below it.
 */
@Composable
fun PageActions(main: PageAction, modifier: Modifier = Modifier, others: List<PageAction> = emptyList()) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        Button(main.label, main.onClick, Modifier.fillMaxWidth(), icon = main.icon)
        others.forEach { action ->
            Button(
                label = action.label,
                onClick = action.onClick,
                modifier = Modifier.fillMaxWidth(),
                style = ButtonStyle.Outlined,
                icon = action.icon,
            )
        }
    }
}
