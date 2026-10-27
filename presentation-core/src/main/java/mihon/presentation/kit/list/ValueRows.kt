package mihon.presentation.kit.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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

/** An explanation in a sheet or dialog, such as what a filter covers. Without [showIcon] it reads as a note. */
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

/**
 * A row whose body is laid out by the caller across the row's width, such as a segmented choice, a pair of buttons or a
 * playground, under an optional [title]. It keeps the inset of the rows beside it, inside a row group card or not.
 */
@Composable
fun ControlRow(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LocalRowInset.current, vertical = KatariTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
    ) {
        if (title != null) Text(title, role = LocalRowDensity.current.titleRole)
        content()
    }
}

/**
 * A group of chips under a group header, such as quick filters in a sheet. It sits between row group cards, with its
 * chips lined up with their edges. [supportingText] explains the choice under the chips, such as why one is disabled.
 */
@Composable
fun ChipsRow(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    content: @Composable FlowRowScope.() -> Unit,
) {
    val bottomPadding = LocalRowDensity.current.verticalPadding
    Column(modifier) {
        RowGroupHeader(title)
        FlowRow(
            modifier = Modifier.padding(
                start = rowGroupEdgeInset(),
                end = rowGroupEdgeInset(),
                bottom = if (supportingText == null) bottomPadding else KatariTheme.spacing.s,
            ),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            content = content,
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                role = TextRole.Supporting,
                modifier = Modifier.padding(
                    start = rowGroupTextInset(),
                    end = rowGroupTextInset(),
                    bottom = bottomPadding,
                ),
            )
        }
    }
}
