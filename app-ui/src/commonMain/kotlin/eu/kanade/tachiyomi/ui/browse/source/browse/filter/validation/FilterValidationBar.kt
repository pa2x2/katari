package eu.kanade.tachiyomi.ui.browse.source.browse.filter.validation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Error
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.ProvideTextRole
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.xs),
        container = SurfaceContainer.ErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = KatariTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            Icon(MaterialSymbols.Rounded.Error, contentDescription = null)
            ProvideTextRole(TextRole.Supporting) {
                Text(
                    text = stringResource(
                        MR.strings.filter_problem_summary,
                        pluralStringResource(MR.plurals.filter_problem_count, count, count),
                        first.displayMessage(),
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(label = stringResource(MR.strings.filter_problem_show), onClick = onShow, style = ButtonStyle.Text)
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
