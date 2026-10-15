package eu.kanade.presentation.library.update.report.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.icerock.moko.resources.StringResource
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
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
    val tone = if (isError) ContentTone.Error else ContentTone.Primary
    Row(
        modifier = modifier.padding(
            start = KatariTheme.spacing.contentInset,
            end = KatariTheme.spacing.contentInset,
            top = KatariTheme.spacing.l,
            bottom = KatariTheme.spacing.xs,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        Icon(imageVector = icon, contentDescription = null, tone = tone)
        Text(
            text = stringResource(title),
            modifier = Modifier.weight(1f),
            role = TextRole.GroupLabel,
            tone = tone,
        )
        Text(
            text = count.toString(),
            role = TextRole.Label,
            tone = ContentTone.Secondary,
        )
        trailing?.invoke()
    }
}
