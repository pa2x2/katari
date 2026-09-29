package eu.kanade.tachiyomi.ui.browse.source.browse.filter.validation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterSheetInsets
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Reports the draft's problems above the filters, outside the scrolling list, so it stays visible on every page of
 * the sheet. [onShow] brings the first problem into view.
 */
@Composable
internal fun FilterValidationBar(validation: FilterValidation, onShow: () -> Unit) {
    val first = validation.issues.firstOrNull() ?: return
    val count = validation.issues.size
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FilterSheetInsets.Horizontal, vertical = 4.dp)
            .background(colors.errorContainer, RoundedCornerShape(12.dp))
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = colors.onErrorContainer)
        Text(
            text = stringResource(
                MR.strings.filter_problem_summary,
                pluralStringResource(MR.plurals.filter_problem_count, count, count),
                first.displayMessage(),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onErrorContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onShow) {
            Text(stringResource(MR.strings.filter_problem_show), color = colors.onErrorContainer)
        }
    }
}

/** Why Apply is disabled, when the reason is something the user must fix. */
@Composable
internal fun filterApplyBlockedReason(validation: FilterValidation, repairNeedsSave: Boolean): String? = when {
    !validation.isValid -> pluralStringResource(
        MR.plurals.filter_fix_problems_to_apply,
        validation.issues.size,
        validation.issues.size,
    )
    repairNeedsSave -> stringResource(MR.strings.filter_save_required)
    else -> null
}
