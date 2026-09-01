package eu.kanade.presentation.more.settings.screen.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.home.navigation.HomeNavigationIcon
import eu.kanade.tachiyomi.ui.home.navigation.HomeNavigationOverflowItems
import eu.kanade.tachiyomi.ui.home.navigation.homeNavigationTitle
import mihon.core.common.HomeScreenTabs
import mihon.core.common.homeScreenContentTabOrder
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun StartupTabSelector(
    draft: HomeNavigationEditorDraft,
    onDraftChange: (HomeNavigationEditorDraft) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val availableTabs = draft.configuration.tabOrder.filter { tab ->
        tab in draft.configuration.enabledTabs && tab in homeScreenContentTabOrder
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = homeNavigationTitle(draft.startupTab),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(MR.strings.home_navigation_startup_tab)) },
            leadingIcon = {
                Icon(Icons.Filled.Home, contentDescription = null)
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            availableTabs.forEach { tab ->
                key(tab) {
                    DropdownMenuItem(
                        text = { Text(homeNavigationTitle(tab)) },
                        leadingIcon = { HomeNavigationIcon(tab = tab, showBadge = false) },
                        onClick = {
                            onDraftChange(draft.copy(startupTab = tab))
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

@Composable
internal fun UnusedDestinations(
    tabs: List<HomeScreenTabs>,
    pendingTab: HomeScreenTabs?,
    dragModifier: (HomeScreenTabs) -> Modifier,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.chunked(2).forEach { rowTabs ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowTabs.forEach { tab ->
                    key(tab) {
                        DestinationTile(
                            tab = tab,
                            isDropPreview = tab == pendingTab,
                            modifier = Modifier
                                .weight(1f)
                                .then(dragModifier(tab)),
                        )
                    }
                }
                if (rowTabs.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DestinationTile(
    tab: HomeScreenTabs,
    isDropPreview: Boolean,
    modifier: Modifier = Modifier,
) {
    val title = homeNavigationTitle(tab)
    OutlinedCard(
        modifier = modifier
            .dropInsertionIndicator(
                visible = isDropPreview,
                edge = DropIndicatorEdge.Around,
                color = MaterialTheme.colorScheme.primary,
            )
            .semantics {
                contentDescription = "$title. Press and hold to move"
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeNavigationIcon(tab = tab, showBadge = false)
            Spacer(Modifier.width(10.dp))
            Text(title, modifier = Modifier.weight(1f), maxLines = 1)
            Icon(
                Icons.Outlined.DragIndicator,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun OverflowDestination(
    tabs: List<HomeScreenTabs>,
    selectedTab: HomeScreenTabs,
    startupTab: HomeScreenTabs,
    targetActive: Boolean,
    targetValid: Boolean,
    insertionIndex: Int?,
    modifier: Modifier,
    dragModifier: (HomeScreenTabs) -> Modifier,
    onClick: (HomeScreenTabs) -> Unit,
) {
    val dropIndicatorColor = if (targetValid) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(
            width = if (targetActive) 2.dp else 1.dp,
            color = if (targetActive) dropIndicatorColor else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = stringResource(MR.strings.home_navigation_overflow),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
            if (tabs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(MR.strings.home_navigation_drop_here),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                HomeNavigationOverflowItems(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onClick = onClick,
                    itemModifier = { tab ->
                        val index = tabs.indexOf(tab)
                        val insertAtEnd = insertionIndex == tabs.size
                        dragModifier(tab).dropInsertionIndicator(
                            visible = insertionIndex == index || (insertAtEnd && index == tabs.lastIndex),
                            edge = if (insertAtEnd) DropIndicatorEdge.Bottom else DropIndicatorEdge.Top,
                            color = dropIndicatorColor,
                        )
                    },
                    trailingContent = { tab ->
                        if (tab == startupTab) {
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = stringResource(MR.strings.home_navigation_startup),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
internal fun DraggedDestination(
    tab: HomeScreenTabs,
    pointerPosition: Offset,
    dropValid: Boolean,
) {
    Surface(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (pointerPosition.x - 72.dp.toPx()).toInt(),
                    y = (pointerPosition.y - 28.dp.toPx()).toInt(),
                )
            }
            .width(144.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (dropValid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (dropValid) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        },
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeNavigationIcon(tab, showBadge = false)
            Spacer(Modifier.width(8.dp))
            Text(homeNavigationTitle(tab), maxLines = 1)
        }
    }
}

internal enum class DropIndicatorEdge {
    Start,
    End,
    Top,
    Bottom,
    Around,
}

internal fun Modifier.dropInsertionIndicator(
    visible: Boolean,
    edge: DropIndicatorEdge,
    color: Color,
): Modifier {
    if (!visible) return this

    return drawWithContent {
        drawContent()
        val strokeWidth = 3.dp.toPx()
        val inset = 8.dp.toPx()
        when (edge) {
            DropIndicatorEdge.Start,
            DropIndicatorEdge.End,
            -> {
                val x = if (edge == DropIndicatorEdge.Start) strokeWidth / 2 else size.width - strokeWidth / 2
                drawLine(
                    color = color,
                    start = Offset(x, inset),
                    end = Offset(x, size.height - inset),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
            DropIndicatorEdge.Top,
            DropIndicatorEdge.Bottom,
            -> {
                val y = if (edge == DropIndicatorEdge.Top) strokeWidth / 2 else size.height - strokeWidth / 2
                drawLine(
                    color = color,
                    start = Offset(inset, y),
                    end = Offset(size.width - inset, y),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
            DropIndicatorEdge.Around -> drawRoundRect(
                color = color,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth),
            )
        }
    }
}
