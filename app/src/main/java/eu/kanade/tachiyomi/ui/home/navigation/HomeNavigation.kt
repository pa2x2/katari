package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.util.fastForEach
import eu.kanade.tachiyomi.R
import mihon.core.common.HomeScreenTabs
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.AccountCircle
import mihon.icons.materialsymbols.rounded.Menu
import mihon.icons.materialsymbols.rounded.QueryStats
import mihon.icons.materialsymbols.rounded.Translate
import mihon.icons.materialsymbols.roundedfilled.Home
import mihon.presentation.kit.content.Badge
import mihon.presentation.kit.content.BadgeTone
import mihon.presentation.kit.content.BadgedBox
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.NotificationBadge
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.navigation.NavigationBar
import mihon.presentation.kit.navigation.NavigationBarItem
import mihon.presentation.kit.navigation.NavigationRail
import mihon.presentation.kit.navigation.NavigationRailItem
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun HomeNavigationBar(
    primaryTabs: List<HomeScreenTabs>,
    overflowTabs: List<HomeScreenTabs>,
    selectedTab: HomeScreenTabs,
    onClick: (HomeScreenTabs) -> Unit,
    modifier: Modifier = Modifier,
    itemModifier: (HomeScreenTabs) -> Modifier = { Modifier },
    overflowModifier: Modifier = Modifier,
    onOverflowClick: (() -> Unit)? = null,
    startupTab: HomeScreenTabs? = null,
) {
    var overflowExpanded by rememberSaveable { mutableStateOf(false) }
    val selectedOverflowTab = selectedTab.takeIf { it in overflowTabs }

    NavigationBar(modifier = modifier) {
        primaryTabs.fastForEach { tab ->
            key(tab) {
                HomeNavigationBarItem(
                    tab = tab,
                    selected = tab == selectedTab,
                    onClick = { onClick(tab) },
                    modifier = itemModifier(tab),
                    isStartup = tab == startupTab,
                )
            }
        }
        if (overflowTabs.isNotEmpty()) {
            HomeNavigationOverflowBarItem(
                tabs = overflowTabs,
                selectedTab = selectedTab,
                selectedOverflowTab = selectedOverflowTab,
                expanded = overflowExpanded,
                onExpandedChange = { overflowExpanded = it },
                onClick = onClick,
                modifier = overflowModifier,
                onOverflowClick = onOverflowClick,
            )
        }
    }
}

@Composable
private fun RowScope.HomeNavigationOverflowBarItem(
    tabs: List<HomeScreenTabs>,
    selectedTab: HomeScreenTabs,
    selectedOverflowTab: HomeScreenTabs?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClick: (HomeScreenTabs) -> Unit,
    modifier: Modifier,
    onOverflowClick: (() -> Unit)?,
) {
    Box(modifier = Modifier.weight(1f).then(modifier)) {
        this@HomeNavigationOverflowBarItem.NavigationBarItem(
            selected = selectedOverflowTab != null,
            onClick = { onOverflowClick?.invoke() ?: onExpandedChange(true) },
            label = stringResource(MR.strings.home_navigation_menu),
            icon = {
                if (rememberAnyHomeTabBadged(tabs)) {
                    BadgedBox(badge = { NotificationBadge() }) { HomeNavigationMenuIcon(selectedOverflowTab) }
                } else {
                    HomeNavigationMenuIcon(selectedOverflowTab)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (onOverflowClick == null) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
            ) {
                HomeNavigationOverflowItems(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onClick = { tab ->
                        onExpandedChange(false)
                        onClick(tab)
                    },
                )
            }
        }
    }
}

@Composable
fun HomeNavigationRail(
    tabs: List<HomeScreenTabs>,
    selectedTab: HomeScreenTabs,
    onClick: (HomeScreenTabs) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(modifier = modifier) {
        tabs.fastForEach { tab ->
            key(tab) {
                NavigationRailItem(
                    selected = tab == selectedTab,
                    onClick = { onClick(tab) },
                    label = homeNavigationTitle(tab),
                    icon = { HomeNavigationIcon(tab) },
                )
            }
        }
    }
}

@Composable
fun HomeNavigationOverflowItems(
    tabs: List<HomeScreenTabs>,
    selectedTab: HomeScreenTabs,
    onClick: (HomeScreenTabs) -> Unit,
    itemModifier: (HomeScreenTabs) -> Modifier = { Modifier },
    trailingContent: (@Composable BoxScope.(HomeScreenTabs) -> Unit)? = null,
) {
    tabs.fastForEach { tab ->
        key(tab) {
            val selected = tab == selectedTab
            DropdownMenuItem(
                label = homeNavigationTitle(tab),
                onClick = { onClick(tab) },
                modifier = itemModifier(tab).then(
                    if (selected) {
                        Modifier.background(KatariTheme.colors.selected, KatariTheme.shapes.medium)
                    } else {
                        Modifier
                    },
                ),
                leading = { HomeNavigationIcon(tab) },
                trailing = trailingContent?.let { content ->
                    { Box { content(tab) } }
                },
            )
        }
    }
}

@Composable
fun HomeNavigationIcon(
    tab: HomeScreenTabs,
    modifier: Modifier = Modifier,
    showBadge: Boolean = true,
) {
    val badge = if (showBadge) rememberHomeTabBadge(tab) else null
    if (badge == null) {
        HomeNavigationBaseIcon(tab, modifier)
        return
    }

    BadgedBox(badge = { HomeTabBadgeContent(badge) }, modifier = modifier) {
        HomeNavigationBaseIcon(tab)
    }
}

@Composable
fun homeNavigationTitle(tab: HomeScreenTabs): String {
    return when (tab) {
        HomeScreenTabs.Library -> stringResource(MR.strings.label_library)
        HomeScreenTabs.Updates -> stringResource(MR.strings.label_recent_updates)
        HomeScreenTabs.History -> stringResource(MR.strings.history)
        HomeScreenTabs.Browse -> stringResource(MR.strings.browse)
        HomeScreenTabs.More -> stringResource(MR.strings.label_more)
        HomeScreenTabs.Profiles -> stringResource(MR.strings.action_switch)
        HomeScreenTabs.Translator -> stringResource(MR.strings.translator_title)
        HomeScreenTabs.Statistics -> stringResource(MR.strings.label_stats)
    }
}

@Composable
private fun RowScope.HomeNavigationBarItem(
    tab: HomeScreenTabs,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isStartup: Boolean = false,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        label = homeNavigationTitle(tab),
        icon = {
            Box {
                HomeNavigationIcon(tab)
                if (isStartup) {
                    val startupDescription = stringResource(MR.strings.home_navigation_startup)
                    Badge(
                        icon = MaterialSymbols.RoundedFilled.Home,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = -KatariTheme.spacing.s, y = -KatariTheme.spacing.xs)
                            .semantics { contentDescription = startupDescription },
                        tone = BadgeTone.Tertiary,
                    )
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun HomeNavigationMenuIcon(selectedOverflowTab: HomeScreenTabs?) {
    AnimatedContent(
        targetState = selectedOverflowTab,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentAlignment = Alignment.Center,
        label = "homeNavigationMenuIcon",
    ) { activeTab ->
        if (activeTab == null) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Menu,
                contentDescription = stringResource(MR.strings.home_navigation_menu),
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomeNavigationIcon(
                    tab = activeTab,
                    modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                    showBadge = false,
                )
                Icon(
                    imageVector = MaterialSymbols.Rounded.Menu,
                    contentDescription = stringResource(MR.strings.home_navigation_menu),
                    modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
private fun HomeNavigationBaseIcon(
    tab: HomeScreenTabs,
    modifier: Modifier = Modifier,
) {
    key(tab) {
        if (tab == HomeScreenTabs.Profiles) {
            Icon(
                imageVector = MaterialSymbols.Rounded.AccountCircle,
                contentDescription = stringResource(MR.strings.profiles_switch_summary),
                modifier = modifier,
            )
        } else if (tab == HomeScreenTabs.Translator) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Translate,
                contentDescription = homeNavigationTitle(tab),
                modifier = modifier,
            )
        } else if (tab == HomeScreenTabs.Statistics) {
            Icon(
                imageVector = MaterialSymbols.Rounded.QueryStats,
                contentDescription = homeNavigationTitle(tab),
                modifier = modifier,
            )
        } else {
            val iconResource = when (tab) {
                HomeScreenTabs.Library -> R.drawable.animated_library
                HomeScreenTabs.Updates -> R.drawable.animated_updates
                HomeScreenTabs.History -> R.drawable.animated_history
                HomeScreenTabs.Browse -> R.drawable.animated_browse_enter
                HomeScreenTabs.More -> R.drawable.animated_more
                HomeScreenTabs.Profiles -> error("Handled above")
                HomeScreenTabs.Translator -> error("Handled above")
                HomeScreenTabs.Statistics -> error("Handled above")
            }
            val image = AnimatedImageVector.animatedVectorResource(iconResource)
            Icon(
                painter = rememberAnimatedVectorPainter(image, atEnd = false),
                contentDescription = homeNavigationTitle(tab),
                modifier = modifier,
            )
        }
    }
}
