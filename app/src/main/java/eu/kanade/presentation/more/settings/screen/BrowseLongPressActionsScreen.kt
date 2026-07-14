package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import mihon.core.common.CustomPreferences
import mihon.core.common.sanitizeBrowseLongPressActionPriority
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.runtime.collectAsState as collectFlowAsState

data class BrowseLongPressActionsScreen(
    private val sourceId: Long? = null,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val customPreferences = remember { Injekt.get<CustomPreferences>() }
        val sourceRepository = remember { Injekt.get<SourceRepository>() }
        val sourceManager = remember { Injekt.get<SourceManager>() }
        val defaultPriority by customPreferences.browseLongPressActionPriority.collectAsState()
        val overrides by customPreferences.browseLongPressActionOverrides.collectAsState()
        val allSources by sourceRepository.getSources().collectFlowAsState(initial = emptyList())
        val catalogueSources = remember(allSources) {
            allSources
                .filter { sourceManager.getCatalogueSource(it.id) != null }
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
                    source = selectedSource,
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
private fun LongPressActionsOverview(
    defaultPriority: List<CustomPreferences.BrowseLongPressAction>,
    overrides: Map<Long, List<CustomPreferences.BrowseLongPressAction>>,
    sources: List<Source>,
    sourceManager: SourceManager,
    onDefaultPriorityChange: (List<CustomPreferences.BrowseLongPressAction>) -> Unit,
    onAddOverride: (Long) -> Unit,
    onOpenOverride: (Long) -> Unit,
    contentPadding: PaddingValues,
) {
    val priority = remember(defaultPriority) {
        sanitizeBrowseLongPressActionPriority(defaultPriority).toMutableStateList()
    }
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState, contentPadding) { from, to ->
        val fromIndex = priority.indexOfFirst { actionKey(it) == from.key }
        val toIndex = priority.indexOfFirst { actionKey(it) == to.key }
        if (fromIndex == -1 || toIndex == -1) return@rememberReorderableLazyListState
        priority.add(toIndex, priority.removeAt(fromIndex))
        onDefaultPriorityChange(priority.toList())
    }
    var showSourcePicker by rememberSaveable { mutableStateOf(false) }
    val sourceById = remember(sources) { sources.associateBy(Source::id) }
    val overriddenSourceIds = overrides.keys.sortedBy { sourceId ->
        sourceById[sourceId]?.visualName ?: sourceManager.getDisplayInfo(sourceId).name
    }

    ScrollbarLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        item {
            SettingsSectionTitle(stringResource(MR.strings.pref_browse_long_press_action_default_profile))
        }
        item {
            Text(
                text = stringResource(MR.strings.pref_browse_long_press_action_priority_summary),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = MaterialTheme.padding.large),
            )
        }
        items(priority, key = ::actionKey) { action ->
            ReorderableItem(reorderableState, actionKey(action)) {
                LongPressActionRow(
                    action = action,
                    supportingText = defaultActionSupportingText(action),
                    showDragHandle = true,
                    dragModifier = Modifier.draggableHandle(),
                )
            }
        }
        item { HorizontalDivider() }
        item {
            SettingsSectionTitle(stringResource(MR.strings.pref_browse_long_press_action_source_overrides))
        }
        if (overrides.isEmpty()) {
            item {
                Text(
                    text = stringResource(MR.strings.pref_browse_long_press_action_no_source_overrides),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.large),
                )
            }
        } else {
            items(overriddenSourceIds, key = { "source:$it" }) { overriddenSourceId ->
                val source = sourceById[overriddenSourceId]
                val displayInfo = sourceManager.getDisplayInfo(overriddenSourceId)
                ListItem(
                    headlineContent = { Text(source?.name ?: displayInfo.name) },
                    supportingContent = {
                        Text(browseLongPressPrioritySummary(overrides.getValue(overriddenSourceId)))
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onOpenOverride(overriddenSourceId) },
                )
            }
        }
        item {
            ListItem(
                headlineContent = {
                    Text(stringResource(MR.strings.pref_browse_long_press_action_add_source_override))
                },
                leadingContent = { Icon(Icons.Outlined.Add, contentDescription = null) },
                modifier = Modifier.clickable { showSourcePicker = true },
            )
        }
    }

    if (showSourcePicker) {
        SourceOverridePicker(
            sources = sources.filterNot { it.id in overrides },
            onSelect = {
                showSourcePicker = false
                onAddOverride(it.id)
            },
            onDismissRequest = { showSourcePicker = false },
        )
    }
}

@Composable
private fun SourceLongPressActions(
    sourceId: Long,
    source: Source?,
    defaultPriority: List<CustomPreferences.BrowseLongPressAction>,
    overridePriority: List<CustomPreferences.BrowseLongPressAction>?,
    sourceManager: SourceManager,
    onPriorityChange: (List<CustomPreferences.BrowseLongPressAction>) -> Unit,
    onUseDefault: () -> Unit,
    contentPadding: PaddingValues,
) {
    val displayedPriority = overridePriority ?: defaultPriority
    val priority = remember(sourceId, displayedPriority) {
        sanitizeBrowseLongPressActionPriority(displayedPriority).toMutableStateList()
    }
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState, contentPadding) { from, to ->
        val fromIndex = priority.indexOfFirst { actionKey(it) == from.key }
        val toIndex = priority.indexOfFirst { actionKey(it) == to.key }
        if (fromIndex == -1 || toIndex == -1) return@rememberReorderableLazyListState
        priority.add(toIndex, priority.removeAt(fromIndex))
        onPriorityChange(priority.toList())
    }
    val supportsImmersive = source?.supportsImmersiveFeed
        ?: (sourceManager.getCatalogueSource(sourceId)?.supportsImmersiveFeed == true)

    ScrollbarLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        item {
            SettingsSectionTitle(
                stringResource(
                    if (overridePriority == null) {
                        MR.strings.pref_browse_long_press_action_using_profile_default
                    } else {
                        MR.strings.pref_browse_long_press_action_custom_for_source
                    },
                ),
            )
        }
        item {
            Text(
                text = stringResource(MR.strings.pref_browse_long_press_action_priority_summary),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = MaterialTheme.padding.large),
            )
        }
        items(priority, key = ::actionKey) { action ->
            if (overridePriority != null) {
                ReorderableItem(reorderableState, actionKey(action)) {
                    LongPressActionRow(
                        action = action,
                        supportingText = sourceActionSupportingText(action, supportsImmersive),
                        showDragHandle = true,
                        dragModifier = Modifier.draggableHandle(),
                    )
                }
            } else {
                LongPressActionRow(
                    action = action,
                    supportingText = sourceActionSupportingText(action, supportsImmersive),
                )
            }
        }
        item {
            TextButton(
                onClick = {
                    if (overridePriority == null) {
                        onPriorityChange(defaultPriority)
                    } else {
                        onUseDefault()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.padding.medium),
            ) {
                Text(
                    stringResource(
                        if (overridePriority == null) {
                            MR.strings.pref_browse_long_press_action_customize_source
                        } else {
                            MR.strings.pref_browse_long_press_action_use_profile_default
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun LongPressActionRow(
    action: CustomPreferences.BrowseLongPressAction,
    supportingText: String,
    showDragHandle: Boolean = false,
    dragModifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(stringResource(action.titleRes)) },
        supportingContent = { Text(supportingText) },
        trailingContent = {
            if (showDragHandle) {
                Icon(
                    imageVector = Icons.Outlined.DragHandle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = dragModifier.padding(MaterialTheme.padding.small),
                )
            }
        },
    )
}

@Composable
private fun defaultActionSupportingText(action: CustomPreferences.BrowseLongPressAction): String {
    return when (action) {
        CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION ->
            stringResource(MR.strings.pref_browse_long_press_action_library_available)
        CustomPreferences.BrowseLongPressAction.PREVIEW ->
            stringResource(MR.strings.pref_browse_long_press_action_preview_available)
        CustomPreferences.BrowseLongPressAction.IMMERSIVE ->
            stringResource(MR.strings.pref_browse_long_press_action_immersive_support_info)
    }
}

@Composable
private fun sourceActionSupportingText(
    action: CustomPreferences.BrowseLongPressAction,
    supportsImmersive: Boolean,
): String {
    return when (action) {
        CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION ->
            stringResource(MR.strings.pref_browse_long_press_action_library_available)
        CustomPreferences.BrowseLongPressAction.PREVIEW ->
            stringResource(MR.strings.pref_browse_long_press_action_preview_available)
        CustomPreferences.BrowseLongPressAction.IMMERSIVE -> stringResource(
            if (supportsImmersive) {
                MR.strings.pref_browse_long_press_action_immersive_supported
            } else {
                MR.strings.pref_browse_long_press_action_immersive_unsupported
            },
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(
            start = MaterialTheme.padding.large,
            top = MaterialTheme.padding.large,
            end = MaterialTheme.padding.large,
        ),
    )
}

@Composable
private fun SourceOverridePicker(
    sources: List<Source>,
    onSelect: (Source) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredSources = remember(sources, query) {
        sources.filter { query.isBlank() || it.visualName.contains(query, ignoreCase = true) }
    }

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.padding.medium),
        ) {
            Text(
                text = stringResource(MR.strings.pref_browse_long_press_action_select_source),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(MR.strings.action_search)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.padding.medium),
            )
            ScrollbarLazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                contentPadding = topSmallPaddingValues,
            ) {
                items(filteredSources, key = Source::id) { source ->
                    BaseSourceItem(
                        source = source,
                        onClickItem = { onSelect(source) },
                    )
                }
            }
        }
    }
}

private fun actionKey(action: CustomPreferences.BrowseLongPressAction): String {
    return "action:${action.name}"
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
