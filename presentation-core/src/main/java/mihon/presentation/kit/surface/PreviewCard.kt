package mihon.presentation.kit.surface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.theme.KatariTheme

/** A playground inside a settings list, such as trying a voice or a recognition engine, below its section header. */
@Composable
fun PreviewCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.s),
        container = SurfaceContainer.Low,
        shape = KatariTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(KatariTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
            content = content,
        )
    }
}
