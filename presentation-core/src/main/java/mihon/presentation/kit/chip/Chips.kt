package mihon.presentation.kit.chip

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.Remove
import mihon.presentation.kit.content.Badge
import mihon.presentation.kit.content.BadgeTone
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.ProvideTextRole
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.LocalContentColor
import mihon.presentation.kit.theme.TextRole
import mihon.presentation.kit.theme.color
import tachiyomi.core.common.preference.TriState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A chip that switches a filter or option on and off; shows a check while [selected] unless it has an [icon].
 * [count] adds a small badge after the label, such as how many filters the chip's sheet has changed; the badge is
 * hidden from accessibility services, so describe it through the chip's state description.
 */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    count: Int? = null,
) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            if (count == null) {
                ChipLabel(label)
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChipLabel(label)
                    Badge(
                        text = count.toString(),
                        modifier = Modifier.clearAndSetSemantics {},
                        tone = BadgeTone.Primary,
                    )
                }
            }
        },
        modifier = modifier,
        enabled = enabled,
        leadingIcon = when {
            icon != null -> {
                { Icon(icon, null, Modifier.size(FilterChipDefaults.IconSize)) }
            }
            selected -> {
                { Icon(MaterialSymbols.Rounded.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
            }
            else -> null
        },
    )
}

/**
 * A chip that starts an action, such as opening a picker. [onClickLabel] tells accessibility services what the tap
 * does when the label alone does not, such as a language chip that changes the language.
 */
@Composable
fun AssistChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
) {
    AssistChip(
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        onClickLabel = onClickLabel,
    ) {
        ChipLabel(label)
    }
}

/**
 * An [AssistChip] whose label is more than plain text, such as a language pair drawn with arrow icons. Text in [label]
 * takes the chip's style when it sets no role, and should keep to one line.
 */
@Composable
fun AssistChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    label: @Composable () -> Unit,
) {
    androidx.compose.material3.AssistChip(
        onClick = onClick,
        label = label,
        // Outer semantics apply after the chip's own click semantics, so this replaces its unlabelled action.
        modifier = if (onClickLabel != null && enabled) {
            modifier.semantics {
                this.onClick(label = onClickLabel) {
                    onClick()
                    true
                }
            }
        } else {
            modifier
        },
        enabled = enabled,
        leadingIcon = icon?.let { { Icon(it, null, Modifier.size(KatariTheme.sizes.iconSmall)) } },
        trailingIcon = trailingIcon?.let { { Icon(it, null, Modifier.size(KatariTheme.sizes.iconSmall)) } },
    )
}

/**
 * A chip for a value the user entered or picked; [onRemove] adds a remove button beside the label. [excluded] marks a
 * value that is filtered out rather than in: it is struck through on the error container so it never reads as picked.
 */
@Composable
fun InputChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    icon: ImageVector? = null,
    onRemove: (() -> Unit)? = null,
    enabled: Boolean = true,
    excluded: Boolean = false,
) {
    val colors = KatariTheme.colors
    InputChip(
        selected = selected,
        onClick = onClick,
        label = { ChipLabel(label, struckThrough = excluded) },
        modifier = modifier,
        enabled = enabled,
        leadingIcon = icon?.let { { Icon(it, null, Modifier.size(KatariTheme.sizes.iconSmall)) } },
        trailingIcon = onRemove?.let {
            {
                Icon(
                    MaterialSymbols.Rounded.Close,
                    contentDescription = stringResource(MR.strings.action_remove),
                    modifier = Modifier
                        .size(KatariTheme.sizes.iconSmall)
                        .clip(KatariTheme.shapes.full)
                        .clickable(enabled = enabled, onClick = it),
                )
            }
        },
        colors = if (excluded) {
            InputChipDefaults.inputChipColors(
                containerColor = colors.errorContainer,
                labelColor = colors.onErrorContainer,
                leadingIconColor = colors.onErrorContainer,
                trailingIconColor = colors.onErrorContainer,
                selectedContainerColor = colors.errorContainer,
                selectedLabelColor = colors.onErrorContainer,
                selectedLeadingIconColor = colors.onErrorContainer,
                selectedTrailingIconColor = colors.onErrorContainer,
            )
        } else {
            InputChipDefaults.inputChipColors()
        },
    )
}

/**
 * A filter chip that can include, exclude or ignore what it names. Tapping runs [onClick], which normally moves to the
 * next state; [onLongClick] is a shortcut such as excluding directly, announced as [onLongClickLabel]. Excluded chips
 * are struck through on the error container so they never read as included.
 */
@Composable
fun TriStateChip(
    label: String,
    state: TriState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
) {
    val stateText = stringResource(
        when (state) {
            TriState.DISABLED -> MR.strings.tristate_not_used
            TriState.ENABLED_IS -> MR.strings.tristate_included
            TriState.ENABLED_NOT -> MR.strings.tristate_excluded
        },
    )
    val (container, content) = when (state) {
        TriState.DISABLED -> SurfaceContainer.None to ContentTone.Secondary
        TriState.ENABLED_IS -> SurfaceContainer.SecondaryContainer to ContentTone.Inherit
        TriState.ENABLED_NOT -> SurfaceContainer.ErrorContainer to ContentTone.Inherit
    }
    Surface(
        modifier = modifier.minimumInteractiveComponentSize(),
        container = container,
        shape = FilterChipDefaults.shape,
        outlined = state == TriState.DISABLED,
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    role = Role.Checkbox,
                    onLongClickLabel = onLongClickLabel,
                    onLongClick = onLongClick,
                    onClick = onClick,
                )
                .semantics {
                    stateDescription = stateText
                    toggleableState = when (state) {
                        TriState.DISABLED -> ToggleableState.Off
                        TriState.ENABLED_IS -> ToggleableState.On
                        TriState.ENABLED_NOT -> ToggleableState.Indeterminate
                    }
                }
                .heightIn(min = FilterChipDefaults.Height)
                .padding(horizontal = KatariTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            when (state) {
                TriState.DISABLED -> Unit
                TriState.ENABLED_IS -> Icon(
                    MaterialSymbols.Rounded.Check,
                    null,
                    Modifier.size(FilterChipDefaults.IconSize),
                )
                TriState.ENABLED_NOT -> Icon(
                    MaterialSymbols.Rounded.Remove,
                    null,
                    Modifier.size(FilterChipDefaults.IconSize),
                )
            }
            ProvideTextRole(TextRole.Label) {
                ChipLabel(label, struckThrough = state == TriState.ENABLED_NOT, tone = content)
            }
        }
    }
}

/** A chip offering a suggested value. */
@Composable
fun SuggestionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    SuggestionChip(
        onClick = onClick,
        label = { ChipLabel(label) },
        modifier = modifier,
        enabled = enabled,
        icon = icon?.let { { Icon(it, null, Modifier.size(KatariTheme.sizes.iconSmall)) } },
    )
}

// Chip labels stay on one line and shorten when the chip is squeezed.
@Composable
private fun ChipLabel(label: String, struckThrough: Boolean = false, tone: ContentTone = ContentTone.Inherit) {
    if (struckThrough) {
        androidx.compose.material3.Text(
            text = label,
            color = tone.color(fallback = LocalContentColor.current),
            textDecoration = TextDecoration.LineThrough,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    } else {
        Text(label, tone = tone, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
