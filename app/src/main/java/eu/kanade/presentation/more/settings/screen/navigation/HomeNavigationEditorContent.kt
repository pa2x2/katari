package eu.kanade.presentation.more.settings.screen.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppSnackbarHost
import eu.kanade.tachiyomi.ui.home.navigation.HomeNavigationBar
import kotlinx.coroutines.launch
import mihon.core.common.HomeScreenTabs
import mihon.core.common.homeScreenTabOrder
import mihon.core.common.navigation.HomeNavigationMoveResult
import mihon.core.common.navigation.HomeNavigationSection
import mihon.core.common.navigation.move
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun HomeNavigationEditorContent(
    draft: HomeNavigationEditorDraft,
    onDraftChange: (HomeNavigationEditorDraft) -> Unit,
    onNavigateUp: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val startupRequiredMessage = stringResource(MR.strings.home_navigation_startup_required)
    val moreRequiredMessage = stringResource(MR.strings.home_navigation_more_required)
    val primaryRequiredMessage = stringResource(MR.strings.home_navigation_primary_required)
    val itemBounds = remember { mutableStateMapOf<HomeScreenTabs, Rect>() }
    var primaryBounds by remember { mutableStateOf(Rect.Zero) }
    var overflowBounds by remember { mutableStateOf(Rect.Zero) }
    var overflowSlotBounds by remember { mutableStateOf(Rect.Zero) }
    var hiddenBounds by remember { mutableStateOf(Rect.Zero) }
    var draggedTab by remember { mutableStateOf<HomeScreenTabs?>(null) }
    var pointerPosition by remember { mutableStateOf(Offset.Unspecified) }
    var dropTarget by remember { mutableStateOf<HomeNavigationDropTarget?>(null) }
    val dropIsValid = when {
        draggedTab == null || dropTarget == null -> true
        dropTarget?.section == HomeNavigationSection.Hidden && draggedTab == draft.startupTab -> false
        dropTarget?.section == HomeNavigationSection.Hidden && draggedTab == HomeScreenTabs.More -> false
        draggedTab in draft.configuration.primaryTabs &&
            draft.configuration.primaryTabs.size == 1 &&
            dropTarget?.section != HomeNavigationSection.Primary
        -> false
        else -> true
    }
    val dropIndicatorColor = if (dropIsValid) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    fun showValidationMessage(message: String) {
        snackbarHostState.currentSnackbarData?.dismiss()
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Indefinite,
            )
        }
    }

    fun resolveDropTarget(position: Offset): HomeNavigationDropTarget? {
        if (overflowBounds.contains(position) || overflowSlotBounds.contains(position)) {
            val index = draft.configuration.overflowTabs.indexOfFirst { tab ->
                val bounds = itemBounds[tab] ?: return@indexOfFirst false
                position.y < bounds.center.y
            }.let { if (it == -1) draft.configuration.overflowTabs.size else it }
            return HomeNavigationDropTarget(HomeNavigationSection.Overflow, index)
        }
        if (hiddenBounds.contains(position)) {
            return HomeNavigationDropTarget(HomeNavigationSection.Hidden, draft.configuration.hiddenTabs.size)
        }
        if (primaryBounds.contains(position)) {
            val index = draft.configuration.primaryTabs.indexOfFirst { tab ->
                val bounds = itemBounds[tab] ?: return@indexOfFirst false
                position.x < bounds.center.x
            }.let { if (it == -1) draft.configuration.primaryTabs.size else it }
            return HomeNavigationDropTarget(HomeNavigationSection.Primary, index)
        }
        return null
    }

    fun applyDrop(tab: HomeScreenTabs, target: HomeNavigationDropTarget?) {
        if (target == null) return
        snackbarHostState.currentSnackbarData?.dismiss()
        if (target.section == HomeNavigationSection.Hidden && tab == draft.startupTab) {
            showValidationMessage(startupRequiredMessage)
            return
        }
        when (val result = draft.configuration.move(tab, target.section, target.index)) {
            is HomeNavigationMoveResult.Moved -> onDraftChange(
                draft.copy(
                    configuration = result.configuration,
                    previewTab = if (
                        target.section == HomeNavigationSection.Hidden &&
                        draft.previewTab == tab
                    ) {
                        draft.startupTab
                    } else {
                        tab
                    },
                ),
            )
            HomeNavigationMoveResult.MoreRequired -> showValidationMessage(moreRequiredMessage)
            HomeNavigationMoveResult.PrimaryRequired -> showValidationMessage(primaryRequiredMessage)
        }
    }

    fun Modifier.navigationDragSource(tab: HomeScreenTabs): Modifier {
        return this
            .onGloballyPositioned { itemBounds[tab] = it.boundsInRoot() }
            .pointerInput(tab, draft.configuration) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localPosition ->
                        draggedTab = tab
                        pointerPosition = (itemBounds[tab]?.topLeft ?: Offset.Zero) + localPosition
                        dropTarget = resolveDropTarget(pointerPosition)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        pointerPosition = (itemBounds[tab]?.topLeft ?: Offset.Zero) + change.position
                        dropTarget = resolveDropTarget(pointerPosition)
                    },
                    onDragCancel = {
                        draggedTab = null
                        pointerPosition = Offset.Unspecified
                        dropTarget = null
                    },
                    onDragEnd = {
                        val tabToMove = draggedTab
                        val target = dropTarget
                        draggedTab = null
                        pointerPosition = Offset.Unspecified
                        dropTarget = null
                        if (tabToMove != null) applyDrop(tabToMove, target)
                    },
                )
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.home_navigation_editor_title),
                    navigateUp = onNavigateUp,
                    actions = {
                        TextButton(onClick = onReset) {
                            Text(stringResource(MR.strings.action_reset))
                        }
                        TextButton(onClick = onSave) {
                            Text(stringResource(MR.strings.action_save))
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                HomeNavigationBar(
                    primaryTabs = draft.configuration.primaryTabs,
                    overflowTabs = draft.configuration.overflowTabs,
                    selectedTab = draft.previewTab,
                    onClick = { onDraftChange(draft.copy(previewTab = it)) },
                    modifier = Modifier.onGloballyPositioned { primaryBounds = it.boundsInRoot() },
                    itemModifier = { tab ->
                        val index = draft.configuration.primaryTabs.indexOf(tab)
                        val insertionIndex = dropTarget
                            ?.takeIf { it.section == HomeNavigationSection.Primary }
                            ?.index
                        val insertAtEnd = insertionIndex == draft.configuration.primaryTabs.size
                        Modifier
                            .navigationDragSource(tab)
                            .dropInsertionIndicator(
                                visible = insertionIndex == index ||
                                    (insertAtEnd && index == draft.configuration.primaryTabs.lastIndex),
                                edge = if (insertAtEnd) DropIndicatorEdge.End else DropIndicatorEdge.Start,
                                color = dropIndicatorColor,
                            )
                    },
                    overflowModifier = Modifier.onGloballyPositioned { overflowSlotBounds = it.boundsInRoot() },
                    onOverflowClick = {},
                    startupTab = draft.startupTab,
                )
            },
        ) { contentPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { hiddenBounds = it.boundsInRoot() }
                        .background(
                            if (dropTarget?.section == HomeNavigationSection.Hidden) {
                                if (dropIsValid) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(MR.strings.home_navigation_editor_instructions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        StartupTabSelector(draft = draft, onDraftChange = onDraftChange)
                        UnusedDestinations(
                            tabs = homeScreenTabOrder.filter { tab ->
                                tab in draft.configuration.hiddenTabs ||
                                    (dropTarget?.section == HomeNavigationSection.Hidden && tab == draggedTab)
                            },
                            pendingTab = draggedTab.takeIf {
                                dropTarget?.section == HomeNavigationSection.Hidden && dropIsValid
                            },
                            dragModifier = { tab ->
                                if (
                                    tab == draggedTab &&
                                    dropTarget?.section == HomeNavigationSection.Hidden &&
                                    tab !in draft.configuration.hiddenTabs
                                ) {
                                    Modifier
                                } else {
                                    Modifier.navigationDragSource(tab)
                                }
                            },
                        )
                    }

                    OverflowDestination(
                        tabs = draft.configuration.overflowTabs,
                        selectedTab = draft.previewTab,
                        startupTab = draft.startupTab,
                        targetActive = dropTarget?.section == HomeNavigationSection.Overflow,
                        targetValid = dropIsValid,
                        insertionIndex = dropTarget
                            ?.takeIf { it.section == HomeNavigationSection.Overflow }
                            ?.index,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .width(224.dp)
                            .onGloballyPositioned { overflowBounds = it.boundsInRoot() },
                        dragModifier = { tab -> Modifier.navigationDragSource(tab) },
                        onClick = { onDraftChange(draft.copy(previewTab = it)) },
                    )
                }

                AppSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .widthIn(max = 280.dp)
                        .padding(start = 16.dp),
                )
            }
        }

        draggedTab?.takeIf { pointerPosition != Offset.Unspecified }?.let { tab ->
            DraggedDestination(
                tab = tab,
                pointerPosition = pointerPosition,
                dropValid = dropIsValid,
            )
        }
    }
}

private data class HomeNavigationDropTarget(
    val section: HomeNavigationSection,
    val index: Int,
)
