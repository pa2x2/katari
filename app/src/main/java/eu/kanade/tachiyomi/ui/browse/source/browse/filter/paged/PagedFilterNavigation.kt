package eu.kanade.tachiyomi.ui.browse.source.browse.filter.paged

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.source.entry.EntryFilterNavigationTarget
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.UnfoldMore
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.navigation.IndexRail
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

internal fun List<EntryFilterNavigationTarget>.usesCompactNavigationRail(): Boolean {
    return size in 3..MAXIMUM_COMPACT_NAVIGATION_TARGETS && all { it.label.length <= 2 }
}

@Composable
internal fun PagedFilterNavigationMenu(
    targets: List<EntryFilterNavigationTarget>,
    onJump: (EntryFilterNavigationTarget) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(horizontal = KatariTheme.spacing.rowInset)) {
        Button(
            label = stringResource(MR.strings.browse_filter_jump_to),
            onClick = { expanded = true },
            style = ButtonStyle.Text,
            icon = MaterialSymbols.Rounded.UnfoldMore,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            targets.forEach { target ->
                DropdownMenuItem(
                    label = target.label,
                    onClick = {
                        expanded = false
                        onJump(target)
                    },
                )
            }
        }
    }
}

@Composable
internal fun PagedFilterNavigationRail(
    targets: List<EntryFilterNavigationTarget>,
    currentTargetId: String?,
    onJump: (EntryFilterNavigationTarget) -> Unit,
) {
    IndexRail(
        labels = targets.map { it.label },
        currentIndex = targets.indexOfFirst { it.id == currentTargetId }.takeIf { it >= 0 },
        onJump = { index -> targets.getOrNull(index)?.let(onJump) },
    )
}

private const val MAXIMUM_COMPACT_NAVIGATION_TARGETS = 32
