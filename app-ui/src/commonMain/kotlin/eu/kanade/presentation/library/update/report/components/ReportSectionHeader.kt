package eu.kanade.presentation.library.update.report.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.icerock.moko.resources.StringResource
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.TextRole
import tachiyomi.presentation.core.i18n.stringResource

/** The header of one outcome of the report, with how many entries had it; [isError] marks failures. */
@Composable
internal fun ReportSectionHeader(
    icon: ImageVector,
    title: StringResource,
    count: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    RowGroupHeader(
        text = stringResource(title),
        modifier = modifier,
        icon = icon,
        tone = if (isError) ContentTone.Error else ContentTone.Inherit,
        actions = {
            Text(text = count.toString(), role = TextRole.Label, tone = ContentTone.Secondary)
            trailing?.invoke()
        },
    )
}
