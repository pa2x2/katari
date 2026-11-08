package app.katari.presentation.kit.button

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.surface.VerticalDivider
import app.katari.presentation.kit.theme.KatariTheme

/** The line between groups of icon buttons in one row, such as text styles and list styles in a formatting bar. */
@Composable
fun ButtonGroupDivider(modifier: Modifier = Modifier) {
    VerticalDivider(
        modifier
            .padding(horizontal = KatariTheme.spacing.xs)
            .height(KatariTheme.spacing.l),
    )
}
