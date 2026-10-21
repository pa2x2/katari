package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

@Composable
internal fun StatisticsInsightTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(KatariTheme.spacing.m)) {
            Text(
                text = value,
                role = TextRole.SectionTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(KatariTheme.spacing.xs))
            Text(text = label, role = TextRole.Supporting)
            detail?.let { Text(text = it, role = TextRole.Badge, tone = ContentTone.Secondary) }
        }
    }
}
