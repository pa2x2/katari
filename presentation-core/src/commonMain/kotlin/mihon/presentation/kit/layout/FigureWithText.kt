package mihon.presentation.kit.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.window.currentWindowLayout

/**
 * A figure, such as a QR code, and the words that go with it, centred: the words under the figure, or beside it in a
 * short window, where under it they would be pushed out of the dialog or sheet it sits in.
 */
@Composable
fun FigureWithText(
    figure: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable ColumnScope.() -> Unit,
) {
    val words: @Composable (Modifier) -> Unit = { wordsModifier ->
        Column(
            modifier = wordsModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
            content = text,
        )
    }
    if (currentWindowLayout().isShort) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            figure()
            words(Modifier.weight(1f))
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        ) {
            figure()
            words(Modifier.fillMaxWidth())
        }
    }
}
