package mihon.translation.provider.server.setup.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/** A note on a setup screen; more than one line reads as numbered steps. */
@Composable
fun ProviderInformationCard(
    title: String? = null,
    lines: List<String>,
) {
    Card(modifier = Modifier.fillMaxWidth(), container = SurfaceContainer.Default) {
        Column(
            modifier = Modifier.padding(KatariTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            title?.let { Text(text = it, role = TextRole.SectionTitle) }
            lines.forEachIndexed { index, line ->
                Text(text = if (lines.size > 1) "${index + 1}. $line" else line, role = TextRole.Body)
            }
        }
    }
}
