package mihon.presentation.kit.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ArrowDownward
import mihon.icons.materialsymbols.rounded.ArrowUpward
import mihon.icons.materialsymbols.rounded.Info
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.Slider
import mihon.presentation.kit.surface.Pill
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/** A setting picked on a slider, with the current value shown as a pill beside the title. */
@Composable
fun SliderRow(
    title: String,
    value: Int,
    valueRange: IntProgression,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    valueText: String = value.toString(),
    steps: Int = with(valueRange) { ((last - first) - 1).coerceAtLeast(0) },
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalRowDensity.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LocalRowInset.current, vertical = density.verticalPadding),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, role = density.titleRole)
                if (subtitle != null) Text(subtitle, role = TextRole.Supporting)
            }
            Pill(valueText)
        }
        Slider(
            value = value,
            onValueChange = { changed ->
                if (changed != value) {
                    onValueChange(changed)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
        )
    }
}

/** A sort option; [descending] null means the list is not sorted by it. */
@Composable
fun SortRow(
    title: String,
    descending: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsRow(
        title = title,
        modifier = modifier,
        leading = {
            when (descending) {
                true -> Icon(MaterialSymbols.Rounded.ArrowDownward, null, tone = ContentTone.Primary)
                false -> Icon(MaterialSymbols.Rounded.ArrowUpward, null, tone = ContentTone.Primary)
                null -> androidx.compose.foundation.layout.Spacer(Modifier.size(KatariTheme.sizes.icon))
            }
        },
        onClick = onClick,
    )
}

/** An explanation under a group of settings. Without [showIcon] it reads as a note within the group. */
@Composable
fun InfoRow(
    text: String,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LocalRowInset.current, vertical = KatariTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
    ) {
        if (showIcon) Icon(MaterialSymbols.Rounded.Info, contentDescription = null, tone = ContentTone.Secondary)
        Text(text, role = TextRole.Supporting)
    }
}

/** A group of chips under a section header, such as quick filters in a sheet. */
@Composable
fun ChipsRow(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable FlowRowScope.() -> Unit,
) {
    Column(modifier) {
        SectionHeader(title)
        FlowRow(
            modifier = Modifier.padding(
                start = LocalRowInset.current,
                end = LocalRowInset.current,
                bottom = LocalRowDensity.current.verticalPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            content = content,
        )
    }
}
