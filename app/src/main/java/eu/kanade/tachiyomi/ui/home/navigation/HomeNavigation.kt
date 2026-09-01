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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import eu.kanade.tachiyomi.R
import mihon.core.common.HomeScreenTabs
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.NavigationBar
import tachiyomi.presentation.core.components.material.NavigationRail
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
            icon = {
                HomeNavigationMenuIcon(selectedOverflowTab)
            },
            label = {
                Text(
                    text = stringResource(MR.strings.home_navigation_menu),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            alwaysShowLabel = true,
        )
        if (onOverflowClick == null) {
            HomeNavigationOverflowMenu(
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
                    icon = { HomeNavigationIcon(tab) },
                    label = {
                        Text(
                            text = homeNavigationTitle(tab),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    alwaysShowLabel = true,
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
                text = {
                    Text(
                        text = homeNavigationTitle(tab),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                onClick = { onClick(tab) },
                leadingIcon = { HomeNavigationIcon(tab) },
                trailingIcon = trailingContent?.let { content ->
                    { Box { content(tab) } }
                },
                modifier = itemModifier(tab)
                    .background(
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                        shape = HomeNavigationOverflowShape,
                    ),
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
    if (!showBadge || (tab != HomeScreenTabs.Updates && tab != HomeScreenTabs.Browse)) {
        HomeNavigationBaseIcon(tab, modifier)
        return
    }

    BadgedBox(
        badge = {
            when (tab) {
                HomeScreenTabs.Updates -> UpdatesBadge()
                HomeScreenTabs.Browse -> BrowseBadge()
            }
        },
        modifier = modifier,
    ) {
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
        icon = {
            Box {
                HomeNavigationIcon(tab)
                if (isStartup) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-7).dp, y = (-5).dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = stringResource(MR.strings.home_navigation_startup),
                            modifier = Modifier.size(10.dp),
                        )
                    }
                }
            }
        },
        label = {
            Text(
                text = homeNavigationTitle(tab),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        alwaysShowLabel = true,
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
                imageVector = Icons.Outlined.Menu,
                contentDescription = stringResource(MR.strings.home_navigation_menu),
            )
        } else {
            Row(
                modifier = Modifier.width(40.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomeNavigationIcon(
                    tab = activeTab,
                    modifier = Modifier.size(20.dp),
                    showBadge = false,
                )
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = stringResource(MR.strings.home_navigation_menu),
                    modifier = Modifier.size(20.dp),
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
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = stringResource(MR.strings.profiles_switch_summary),
                modifier = modifier,
            )
        } else if (tab == HomeScreenTabs.Translator) {
            Icon(
                imageVector = Icons.Outlined.Translate,
                contentDescription = homeNavigationTitle(tab),
                modifier = modifier,
            )
        } else if (tab == HomeScreenTabs.Statistics) {
            Icon(
                imageVector = Icons.Outlined.QueryStats,
                contentDescription = homeNavigationTitle(tab),
                modifier = modifier,
            )
        } else {
            val iconResource = when (tab) {
                HomeScreenTabs.Library -> R.drawable.anim_library_enter
                HomeScreenTabs.Updates -> R.drawable.anim_updates_enter
                HomeScreenTabs.History -> R.drawable.anim_history_enter
                HomeScreenTabs.Browse -> R.drawable.anim_browse_enter
                HomeScreenTabs.More -> R.drawable.anim_more_enter
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
