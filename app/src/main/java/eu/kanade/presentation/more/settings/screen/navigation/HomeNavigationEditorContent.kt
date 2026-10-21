package eu.kanade.presentation.more.settings.screen.navigation

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalDensity
import eu.kanade.tachiyomi.ui.home.navigation.HomeNavigationBar
import kotlinx.coroutines.launch
import mihon.core.common.HomeScreenTabs
import mihon.core.common.homeScreenTabOrder
import mihon.core.common.navigation.HomeNavigationMoveResult
import mihon.core.common.navigation.HomeNavigationSection
import mihon.core.common.navigation.move
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.control.DropEdge
import mihon.presentation.kit.control.dropInsertionIndicator
import mihon.presentation.kit.feedback.SnackbarDuration
import mihon.presentation.kit.feedback.SnackbarHost
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupNote
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.background
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun HomeNavigationEditorContent(
    draft: HomeNavigationEditorDraft,
    onDraftChange: (HomeNavigationEditorDraft) -> Unit,
    onNavigateUp: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    resetEnabled: Boolean,
    saveEnabled: Boolean,
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
    // Lets the last unused destination scroll clear of the overflow panel floating over the list.
    val overflowClearance = with(LocalDensity.current) { overflowBounds.height.toDp() } + KatariTheme.spacing.l

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
                        Button(
                            label = stringResource(MR.strings.action_reset),
                            onClick = onReset,
                            style = ButtonStyle.Text,
                            enabled = resetEnabled,
                        )
                        Button(
                            label = stringResource(MR.strings.action_save),
                            onClick = onSave,
                            style = ButtonStyle.Text,
                            enabled = saveEnabled,
                        )
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
                                edge = if (insertAtEnd) DropEdge.End else DropEdge.Start,
                                valid = dropIsValid,
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
                            if (dropTarget?.section == HomeNavigationSection.Hidden && dropIsValid) {
                                SurfaceContainer.Default
                            } else {
                                SurfaceContainer.Surface
                            },
                        ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = overflowClearance),
                        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
                    ) {
                        RowGroupNote(text = stringResource(MR.strings.home_navigation_editor_instructions))
                        RowGroup(rows = listOf { StartupTabSelector(draft = draft, onDraftChange = onDraftChange) })
                        UnusedDestinations(
                            modifier = Modifier.padding(horizontal = KatariTheme.spacing.contentInset),
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
                            .padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s)
                            .fillMaxWidth(OVERFLOW_PANEL_WIDTH_FRACTION)
                            .onGloballyPositioned { overflowBounds = it.boundsInRoot() },
                        dragModifier = { tab -> Modifier.navigationDragSource(tab) },
                        onClick = { onDraftChange(draft.copy(previewTab = it)) },
                    )
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(1f - OVERFLOW_PANEL_WIDTH_FRACTION),
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

// The overflow panel mirrors the navigation bar's overflow menu at the bottom end; the snackbar uses the space beside it.
private const val OVERFLOW_PANEL_WIDTH_FRACTION = 0.55f
