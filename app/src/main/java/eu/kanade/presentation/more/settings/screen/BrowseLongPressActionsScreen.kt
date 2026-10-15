package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.browse.longpress.LongPressActionsOverview
import eu.kanade.presentation.more.settings.screen.browse.longpress.SourceLongPressActions
import eu.kanade.presentation.util.Screen
import mihon.app.di.appGraph
import mihon.core.common.CustomPreferences
import mihon.core.common.sanitizeBrowseLongPressActionPriority
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import androidx.compose.runtime.collectAsState as collectFlowAsState

data class BrowseLongPressActionsScreen(
    private val sourceId: Long? = null,
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val customPreferences = remember { context.appGraph.customPreferences }
        val sourceRepository = remember { context.appGraph.sourceRepository }
        val sourceManager = remember { context.appGraph.sourceManager }
        val defaultPriority by customPreferences.browseLongPressActionPriority.collectAsState()
        val overrides by customPreferences.browseLongPressActionOverrides.collectAsState()
        val allSources by sourceRepository.getSources().collectFlowAsState(initial = emptyList())
        val catalogueSources = remember(allSources) {
            allSources
                .filter { it.catalogue != null }
                .distinctBy(Source::id)
                .sortedBy { it.visualName.lowercase() }
        }
        val selectedSource = sourceId?.let { id -> catalogueSources.firstOrNull { it.id == id } }
        val title = if (sourceId == null) {
            stringResource(MR.strings.pref_browse_long_press_action)
        } else {
            selectedSource?.name ?: sourceManager.getDisplayInfo(sourceId).name
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = title,
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            if (sourceId == null) {
                LongPressActionsOverview(
                    defaultPriority = defaultPriority,
                    overrides = overrides,
                    sources = catalogueSources,
                    sourceManager = sourceManager,
                    onDefaultPriorityChange = customPreferences.browseLongPressActionPriority::set,
                    onAddOverride = { id ->
                        customPreferences.setBrowseLongPressActionOverride(id, defaultPriority)
                        navigator.push(BrowseLongPressActionsScreen(id))
                    },
                    onOpenOverride = { navigator.push(BrowseLongPressActionsScreen(it)) },
                    contentPadding = contentPadding,
                )
            } else {
                SourceLongPressActions(
                    sourceId = sourceId,
                    defaultPriority = defaultPriority,
                    overridePriority = overrides[sourceId],
                    sourceManager = sourceManager,
                    onPriorityChange = { customPreferences.setBrowseLongPressActionOverride(sourceId, it) },
                    onUseDefault = { customPreferences.clearBrowseLongPressActionOverride(sourceId) },
                    contentPadding = contentPadding,
                )
            }
        }
    }
}

@Composable
internal fun browseLongPressPrioritySummary(
    priority: Collection<CustomPreferences.BrowseLongPressAction>,
): String {
    val labels = sanitizeBrowseLongPressActionPriority(priority).map { action ->
        stringResource(action.titleRes)
    }
    return labels.joinToString(" → ")
}
