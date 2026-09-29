package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.Button
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun SourceFilterSheetFooter(
    onReset: () -> Unit,
    resetEnabled: Boolean,
    onApply: () -> Unit,
    applyEnabled: Boolean,
    applyBlockedReason: String? = null,
) {
    SourceFilterSheetActionRow {
        TextButton(onClick = onReset, enabled = resetEnabled) {
            Text(stringResource(MR.strings.filter_reset_defaults))
        }
        Box(Modifier.weight(1f).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterEnd) {
            applyBlockedReason?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                )
            }
        }
        Button(onClick = onApply, enabled = applyEnabled) {
            Text(stringResource(MR.strings.action_apply))
        }
    }
}
