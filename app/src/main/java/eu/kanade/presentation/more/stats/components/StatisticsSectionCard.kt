package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.CardStyle
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

@Composable
internal fun StatisticsSectionCard(
    title: String? = null,
    trailingText: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    contentSpacing: Dp = KatariTheme.spacing.l,
    showContent: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hasHeader = title != null || trailingText != null || (actionLabel != null && onActionClick != null)
    Card(Modifier.fillMaxWidth(), style = CardStyle.Outlined) {
        Column(Modifier.padding(KatariTheme.spacing.contentInset)) {
            if (hasHeader) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    title?.let {
                        Text(text = it, modifier = Modifier.weight(1f), role = TextRole.SectionTitle)
                    } ?: Spacer(Modifier.weight(1f))
                    trailingText?.let { Text(text = it, role = TextRole.Badge, tone = ContentTone.Secondary) }
                    if (actionLabel != null && onActionClick != null) {
                        Button(label = actionLabel, onClick = onActionClick, style = ButtonStyle.Text)
                    }
                }
            }
            if (showContent) {
                if (hasHeader) Spacer(Modifier.height(contentSpacing))
                content()
            }
        }
    }
}
