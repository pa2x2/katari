package mihon.entry.viewer.chrome.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.navigation.FloatingToolbar
import mihon.presentation.kit.theme.KatariTheme

/**
 * A reader's controls as one floating row, for windows wider than a phone where a slider spanning the window would be
 * long and a second bar of actions would cover more of the page: raised, at most a readable width wide and centred
 * over the page, clear of the navigation bar and a side cutout. [above] stacks over the row, such as a return bar.
 */
@Composable
fun ReaderFloatingControlRow(
    modifier: Modifier = Modifier,
    above: @Composable ColumnScope.() -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            )
            .padding(bottom = KatariTheme.spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        above()
        FloatingToolbar(modifier = Modifier.widthIn(max = KatariTheme.sizes.readableWidth), content = content)
    }
}
