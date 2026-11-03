package eu.kanade.presentation.more.settings.screen.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import eu.kanade.tachiyomi.ui.home.navigation.HomeNavigationIcon
import eu.kanade.tachiyomi.ui.home.navigation.HomeNavigationOverflowItems
import eu.kanade.tachiyomi.ui.home.navigation.homeNavigationTitle
import mihon.core.common.HomeScreenTabs
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.DragIndicator
import mihon.icons.materialsymbols.roundedfilled.Home
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.DragPreview
import mihon.presentation.kit.control.DropEdge
import mihon.presentation.kit.control.DropTargetPanel
import mihon.presentation.kit.control.dropInsertionIndicator
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.CardStyle
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun UnusedDestinations(
    tabs: List<HomeScreenTabs>,
    pendingTab: HomeScreenTabs?,
    dragModifier: (HomeScreenTabs) -> Modifier,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        tabs.chunked(2).forEach { rowTabs ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
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
    Card(
        modifier = modifier.semantics {
            contentDescription = "$title. Press and hold to move"
        },
        style = CardStyle.Outlined,
        highlighted = isDropPreview,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KatariTheme.spacing.m),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeNavigationIcon(tab = tab, showBadge = false)
            Text(title, modifier = Modifier.weight(1f), role = TextRole.ItemTitle, maxLines = 1)
            Icon(MaterialSymbols.Rounded.DragIndicator, contentDescription = null, tone = ContentTone.Secondary)
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
    DropTargetPanel(active = targetActive, valid = targetValid, modifier = modifier) {
        Text(
            text = stringResource(MR.strings.home_navigation_overflow),
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.xs),
            role = TextRole.Label,
            tone = ContentTone.Secondary,
        )
        if (tabs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = KatariTheme.spacing.xl),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(MR.strings.home_navigation_drop_here),
                    role = TextRole.Body,
                    tone = ContentTone.Secondary,
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
                        edge = if (insertAtEnd) DropEdge.Bottom else DropEdge.Top,
                        valid = targetValid,
                    )
                },
                trailingContent = { tab ->
                    if (tab == startupTab) {
                        Icon(
                            MaterialSymbols.RoundedFilled.Home,
                            contentDescription = stringResource(MR.strings.home_navigation_startup),
                            modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                        )
                    }
                },
            )
        }
    }
}

/** The destination being dragged, centred under the finger. */
@Composable
internal fun DraggedDestination(
    tab: HomeScreenTabs,
    pointerPosition: Offset,
    dropValid: Boolean,
) {
    DragPreview(pointerPosition = pointerPosition, valid = dropValid) {
        HomeNavigationIcon(tab, showBadge = false)
        Text(homeNavigationTitle(tab), maxLines = 1)
    }
}
