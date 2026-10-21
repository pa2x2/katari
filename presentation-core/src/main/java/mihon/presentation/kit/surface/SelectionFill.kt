package mihon.presentation.kit.surface

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.theme.KatariTheme

/** Fills a row or item while it is [selected], such as an entry picked for a bulk action. */
@Composable
fun Modifier.selectionFill(selected: Boolean): Modifier =
    if (selected) background(KatariTheme.colors.selected) else this
