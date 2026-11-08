package app.katari.sourcemigration.review.components

import androidx.compose.runtime.Composable
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Info
import app.katari.presentation.kit.button.IconButton
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun SourceMigrationEntryDetailsButton(
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(
        icon = MaterialSymbols.Rounded.Info,
        contentDescription = stringResource(MR.strings.action_details),
        onClick = onClick,
        enabled = enabled,
    )
}
