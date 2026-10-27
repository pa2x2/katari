package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.util.fastForEach
import eu.kanade.presentation.util.rememberAnimatedVectorPainter
import eu.kanade.tachiyomi.R
import mihon.core.common.HomeScreenTabs
import mihon.feature.profiles.ui.components.ProfileAvatar
import mihon.feature.profiles.ui.switcher.LocalProfileSwitcher
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
import mihon.presentation.kit.navigation.DockPlacement
import mihon.presentation.kit.navigation.DockScope
import mihon.presentation.kit.navigation.FloatingDock
import mihon.presentation.kit.navigation.FloatingDockItem
import mihon.presentation.kit.surface.AvatarSize
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
    placement: DockPlacement = DockPlacement.Bottom,
) {
    var overflowExpanded by rememberSaveable { mutableStateOf(false) }
    val selectedOverflowTab = selectedTab.takeIf { it in overflowTabs }

    FloatingDock(modifier = modifier, placement = placement) {
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
private fun DockScope.HomeNavigationOverflowBarItem(
    tabs: List<HomeScreenTabs>,
    selectedTab: HomeScreenTabs,
    selectedOverflowTab: HomeScreenTabs?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClick: (HomeScreenTabs) -> Unit,
    modifier: Modifier,
    onOverflowClick: (() -> Unit)?,
) {
    FloatingDockItem(
        selected = selectedOverflowTab != null,
        onClick = { onOverflowClick?.invoke() ?: onExpandedChange(true) },
        label = selectedOverflowTab?.let { homeNavigationTitle(it) } ?: stringResource(MR.strings.home_navigation_menu),
        icon = {
            if (rememberAnyHomeTabBadged(tabs)) {
                BadgedBox(badge = { NotificationBadge() }) { HomeNavigationMenuIcon(selectedOverflowTab) }
            } else {
                HomeNavigationMenuIcon(selectedOverflowTab)
            }
        },
        modifier = modifier.testTag(OVERFLOW_TEST_TAG),
        menu = if (onOverflowClick == null) {
            {
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
        } else {
            null
        },
    )
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
                modifier = itemModifier(tab).testTag(tab.testTag),
                selected = selected,
                leading = { HomeNavigationIcon(tab, selected = selected) },
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
    selected: Boolean = false,
    showBadge: Boolean = true,
) {
    val badge = if (showBadge) rememberHomeTabBadge(tab) else null
    if (badge == null) {
        HomeNavigationBaseIcon(tab, selected, modifier)
        return
    }

    BadgedBox(badge = { HomeTabBadgeContent(badge) }, modifier = modifier) {
        HomeNavigationBaseIcon(tab, selected)
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
        HomeScreenTabs.Profiles ->
            LocalProfileSwitcher.current?.activeProfile?.name
                ?: stringResource(MR.strings.action_switch)
        HomeScreenTabs.Translator -> stringResource(MR.strings.translator_title)
        HomeScreenTabs.Statistics -> stringResource(MR.strings.label_stats)
    }
}

@Composable
private fun DockScope.HomeNavigationBarItem(
    tab: HomeScreenTabs,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isStartup: Boolean = false,
) {
    FloatingDockItem(
        selected = selected,
        onClick = onClick,
        label = homeNavigationTitle(tab),
        icon = {
            Box {
                HomeNavigationIcon(tab, selected = selected)
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
        modifier = modifier.testTag(tab.testTag),
    )
}

/**
 * Names a destination wherever it sits, in the dock or the dock's menu, and whatever its label reads. UI
 * automation, such as the showcase capture scenes, finds destinations by these.
 */
private val HomeScreenTabs.testTag: String get() = "home_tab_${name.lowercase()}"

private const val OVERFLOW_TEST_TAG = "home_tab_overflow"

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
                    selected = true,
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
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    key(tab) {
        val profileSwitcher = LocalProfileSwitcher.current
        if (tab == HomeScreenTabs.Profiles && profileSwitcher != null) {
            ProfileAvatar(
                colorSeed = profileSwitcher.activeProfile.colorSeed,
                size = AvatarSize.Icon,
                modifier = modifier,
            )
        } else if (tab == HomeScreenTabs.Profiles) {
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
        } else if (tab == HomeScreenTabs.Browse || tab == HomeScreenTabs.More) {
            // These have their own way out, played forwards rather than reversing the way in
            val (enter, exit) = if (tab == HomeScreenTabs.Browse) {
                R.drawable.animated_browse_enter to R.drawable.animated_browse_exit
            } else {
                R.drawable.animated_more_enter to R.drawable.animated_more_exit
            }
            Icon(
                painter = rememberAnimatedVectorPainter(
                    enter = AnimatedImageVector.animatedVectorResource(enter),
                    exit = AnimatedImageVector.animatedVectorResource(exit),
                    atEnd = selected,
                ),
                contentDescription = homeNavigationTitle(tab),
                modifier = modifier,
            )
        } else {
            // Each animation ends on the filled icon that marks the selected destination
            val iconResource = when (tab) {
                HomeScreenTabs.Library -> R.drawable.animated_library
                HomeScreenTabs.Updates -> R.drawable.animated_updates
                HomeScreenTabs.History -> R.drawable.animated_history
                HomeScreenTabs.Browse -> error("Handled above")
                HomeScreenTabs.More -> error("Handled above")
                HomeScreenTabs.Profiles -> error("Handled above")
                HomeScreenTabs.Translator -> error("Handled above")
                HomeScreenTabs.Statistics -> error("Handled above")
            }
            val image = AnimatedImageVector.animatedVectorResource(iconResource)
            Icon(
                painter = rememberAnimatedVectorPainter(image, atEnd = selected),
                contentDescription = homeNavigationTitle(tab),
                modifier = modifier,
            )
        }
    }
}
