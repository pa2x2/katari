package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun StatisticsSectionCard(
    title: String? = null,
    trailingText: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    contentSpacing: Dp = 18.dp,
    showContent: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hasHeader = title != null || trailingText != null || (actionLabel != null && onActionClick != null)
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            if (hasHeader) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    title?.let {
                        Text(
                            text = it,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    } ?: Spacer(Modifier.weight(1f))
                    trailingText?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (actionLabel != null && onActionClick != null) {
                        TextButton(onClick = onActionClick) { Text(actionLabel) }
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
