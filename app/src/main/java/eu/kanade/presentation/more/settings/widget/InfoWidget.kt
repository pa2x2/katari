package eu.kanade.presentation.more.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.list.InfoRow

@Composable
internal fun InfoWidget(
    text: String,
    showIcon: Boolean = true,
) {
    InfoRow(text = text, modifier = Modifier.preferenceHighlight(), showIcon = showIcon)
}
