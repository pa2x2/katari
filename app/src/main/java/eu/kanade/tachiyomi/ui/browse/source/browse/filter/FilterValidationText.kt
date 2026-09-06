package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValidationCode
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValidationIssue
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun EntryFilterValidationIssue.displayMessage(): String = message ?: stringResource(
    when (code) {
        EntryFilterValidationCode.REQUIRED -> MR.strings.filter_value_required
        EntryFilterValidationCode.INVALID_DATE -> MR.strings.filter_date_invalid
        EntryFilterValidationCode.DATE_PRECISION -> MR.strings.filter_date_precision_invalid
        EntryFilterValidationCode.DATE_BOUNDS -> MR.strings.filter_date_bounds_invalid
        EntryFilterValidationCode.DATE_RANGE -> MR.strings.filter_date_range_invalid
        EntryFilterValidationCode.SOURCE -> MR.strings.filter_value_invalid
    },
)
