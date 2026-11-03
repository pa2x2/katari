package mihon.entry.viewer.chrome.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.navigation.AppBar

/** Shared reader top bar with feature-owned actions. */
@Composable
fun ReaderChromeTopBar(
    title: String?,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    AppBar(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        navigateUp = navigateUp,
        actions = actions,
        overlay = true,
    )
}
