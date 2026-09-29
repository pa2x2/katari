package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.Button
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun SourceFilterSheetFooter(
    onReset: () -> Unit,
    resetEnabled: Boolean,
    onApply: () -> Unit,
    applyEnabled: Boolean,
) {
    SourceFilterSheetActionRow {
        TextButton(onClick = onReset, enabled = resetEnabled) {
            Text(stringResource(MR.strings.filter_reset_defaults))
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onApply, enabled = applyEnabled) {
            Text(stringResource(MR.strings.action_apply))
        }
    }
}
