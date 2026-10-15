package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Reset and apply under the filter sheet, with why applying is blocked when it is. */
@Composable
internal fun SourceFilterSheetFooter(
    onReset: () -> Unit,
    resetEnabled: Boolean,
    onApply: () -> Unit,
    applyEnabled: Boolean,
    applyBlockedReason: String? = null,
    applyLabel: String = stringResource(MR.strings.action_apply),
) {
    Surface(container = SurfaceContainer.Background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KatariTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                label = stringResource(MR.strings.filter_reset_defaults),
                onClick = onReset,
                style = ButtonStyle.Text,
                enabled = resetEnabled,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = KatariTheme.spacing.s),
                contentAlignment = Alignment.CenterEnd,
            ) {
                applyBlockedReason?.let {
                    Text(
                        text = it,
                        role = TextRole.Supporting,
                        tone = ContentTone.Error,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                    )
                }
            }
            Button(label = applyLabel, onClick = onApply, enabled = applyEnabled)
        }
    }
}
