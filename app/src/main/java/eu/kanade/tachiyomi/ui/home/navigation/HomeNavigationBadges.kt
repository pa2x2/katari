package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import mihon.app.di.appGraph
import mihon.core.common.HomeScreenTabs
import mihon.presentation.kit.content.NotificationBadge
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** What a tab's icon is marked with: a count, or a dot when there's something new without a number. */
internal sealed interface HomeTabBadge {
    val description: String

    data class Count(val count: Int, override val description: String) : HomeTabBadge

    data class Dot(override val description: String) : HomeTabBadge
}

@Composable
internal fun rememberHomeTabBadge(tab: HomeScreenTabs): HomeTabBadge? = when (tab) {
    HomeScreenTabs.Updates -> rememberUpdatesBadge()
    HomeScreenTabs.Browse -> rememberBrowseBadge()
    HomeScreenTabs.More, HomeScreenTabs.Statistics -> rememberYearRecapBadge()
    HomeScreenTabs.Library, HomeScreenTabs.History, HomeScreenTabs.Profiles, HomeScreenTabs.Translator -> null
}

/** Whether any of [tabs] is marked, so the menu holding them can say so while it's closed. */
@Composable
internal fun rememberAnyHomeTabBadged(tabs: List<HomeScreenTabs>): Boolean {
    var badged = false
    tabs.fastForEach { tab ->
        key(tab) { if (rememberHomeTabBadge(tab) != null) badged = true }
    }
    return badged
}

@Composable
internal fun HomeTabBadgeContent(badge: HomeTabBadge, modifier: Modifier = Modifier) {
    when (badge) {
        is HomeTabBadge.Count -> NotificationBadge(
            modifier = modifier.semantics { contentDescription = badge.description },
            count = badge.count,
        )
        is HomeTabBadge.Dot -> NotificationBadge(modifier.semantics { contentDescription = badge.description })
    }
}

@Composable
private fun rememberUpdatesBadge(): HomeTabBadge? {
    val context = LocalContext.current
    val count by produceState(initialValue = 0) {
        val preferences = context.appGraph.libraryPreferences
        combine(
            preferences.newShowUpdatesCount.changes(),
            preferences.newUpdatesCount.changes(),
        ) { show, updates -> if (show) updates else 0 }
            .collectLatest { value = it }
    }
    if (count <= 0) return null
    return HomeTabBadge.Count(
        count = count,
        description = pluralStringResource(MR.plurals.notification_updates_generic, count = count, count),
    )
}

@Composable
private fun rememberBrowseBadge(): HomeTabBadge? {
    val context = LocalContext.current
    val count by produceState(initialValue = 0) {
        val extensionManager = context.appGraph.extensionManager
        combine(
            extensionManager.pendingUpdatesCount,
            extensionManager.isAutoUpdateInProgress,
        ) { updates, inProgress -> if (inProgress) 0 else updates }
            .collectLatest { value = it }
    }
    if (count <= 0) return null
    return HomeTabBadge.Count(
        count = count,
        description = pluralStringResource(MR.plurals.update_check_notification_ext_updates, count = count, count),
    )
}

@Composable
private fun rememberYearRecapBadge(): HomeTabBadge? {
    val context = LocalContext.current
    val unopened by produceState(initialValue = false) {
        context.appGraph.statisticsUnopenedYearRecap.subscribe().map { it != null }.collectLatest { value = it }
    }
    if (!unopened) return null
    return HomeTabBadge.Dot(stringResource(MR.strings.statistics_recap_new))
}
