package eu.kanade.tachiyomi.ui.browse.immersive

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.compose.AsyncImage
import eu.kanade.presentation.browse.immersive.EntryImmersivePositionState
import eu.kanade.presentation.entry.entryTypePresentation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mihon.entry.interactions.EntryImmersiveHandle
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.asEntryCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
internal fun EntryImmersiveContent(
    itemCount: Int,
    itemIdentity: (Int) -> EntryImmersiveItemKey?,
    itemContent: @Composable (Int) -> Entry?,
    immersiveModel: EntryImmersiveScreenModel,
    contextLabel: String,
    contextLeadingContent: (@Composable () -> Unit)? = null,
    onContextClick: (() -> Unit)?,
    onExitImmersive: () -> Unit,
    onEntryClick: (Entry) -> Unit,
    onLibraryAction: (Entry) -> Unit,
    onZoomStateChange: (Boolean) -> Unit,
    positionState: EntryImmersivePositionState,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pageRequest: Int = 0,
    onPageSettled: (Int) -> Unit = {},
    onNearEnd: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    overlayContent: @Composable BoxScope.(PagerState) -> Unit = {},
) {
    val context = LocalContext.current
    val immersiveState by immersiveModel.state.collectAsState()
    val initialPageInBounds = positionState.itemIndex.coerceIn(0, (itemCount - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = initialPageInBounds) { itemCount }
    val scope = rememberCoroutineScope()
    val entryOpenInteraction = remember { Injekt.get<mihon.entry.interactions.EntryOpenInteraction>() }
    val currentOnPageSettled by rememberUpdatedState(onPageSettled)
    val currentOnNearEnd by rememberUpdatedState(onNearEnd)
    var isZoomed by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(false) }

    val retainedItemKeys = buildSet {
        pagerState.settledPage
            .takeIf { it in 0 until itemCount }
            ?.let(itemIdentity)
            ?.let(::add)
        for (page in pagerState.currentPage - PRELOAD_RADIUS..pagerState.currentPage + PRELOAD_RADIUS) {
            if (page in 0 until itemCount) itemIdentity(page)?.let(::add)
        }
    }
    LaunchedEffect(retainedItemKeys) {
        immersiveModel.retain(retainedItemKeys)
    }

    LaunchedEffect(pageRequest) {
        if (pageRequest > 0 && itemCount > 0) pagerState.animateScrollToPage(0)
    }

    LaunchedEffect(itemCount) {
        if (itemCount > 0 && pagerState.currentPage >= itemCount) {
            pagerState.scrollToPage(itemCount - 1)
        }
    }

    LaunchedEffect(pagerState, itemCount) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collectLatest { page ->
                positionState.updateItemIndex(page)
                currentOnPageSettled(page)
                if (page >= itemCount - LOAD_MORE_PAGE_THRESHOLD - 1) currentOnNearEnd(page)
            }
    }

    PullRefresh(
        refreshing = refreshing,
        enabled = shouldEnableImmersivePullRefresh(pagerState.settledPage, isZoomed),
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                key = { page ->
                    itemIdentity(page)?.let(::entryImmersiveItemKey) ?: "immersive:$page"
                },
                userScrollEnabled = !isZoomed,
                beyondViewportPageCount = PRELOAD_RADIUS,
            ) { page ->
                val entry = itemContent(page)
                val itemKey = entry?.immersiveItemKey() ?: itemIdentity(page)
                val itemState = itemKey?.let(immersiveState.items::get)
                val isActive = page == pagerState.settledPage
                val preloadRange = (pagerState.currentPage - PRELOAD_RADIUS)..(pagerState.currentPage + PRELOAD_RADIUS)

                if (entry != null && (page in preloadRange || isActive)) {
                    LaunchedEffect(itemKey) { immersiveModel.load(context, entry) }
                }

                EntryImmersivePage(
                    entry = entry,
                    itemState = itemState,
                    isActive = isActive,
                    immersiveModel = immersiveModel,
                    contextLabel = contextLabel,
                    contextLeadingContent = contextLeadingContent,
                    controlsVisible = controlsVisible,
                    onToggleControls = { controlsVisible = !controlsVisible },
                    onContextClick = onContextClick,
                    onExitImmersive = onExitImmersive,
                    onEntryClick = onEntryClick,
                    onLibraryAction = onLibraryAction,
                    showBackToTop = pagerState.currentPage > 0,
                    onBackToTop = { scope.launch { pagerState.animateScrollToPage(0) } },
                    onOpenChapter = if (entry != null && itemState is EntryImmersiveScreenModel.ItemState.Ready) {
                        {
                            entryOpenInteraction.open(
                                context = context,
                                entry = entry,
                                chapter = itemState.chapter,
                            )
                        }
                    } else {
                        null
                    },
                    onZoomStateChange = { zoomed ->
                        isZoomed = zoomed
                        onZoomStateChange(zoomed)
                    },
                    onRetry = { entry?.let { immersiveModel.retry(context, it) } },
                )
            }

            overlayContent(pagerState)
        }
    }
}

internal fun shouldEnableImmersivePullRefresh(settledPage: Int, isZoomed: Boolean): Boolean {
    return settledPage == 0 && !isZoomed
}

@Composable
private fun EntryImmersivePage(
    entry: Entry?,
    itemState: EntryImmersiveScreenModel.ItemState?,
    isActive: Boolean,
    immersiveModel: EntryImmersiveScreenModel,
    contextLabel: String,
    contextLeadingContent: (@Composable () -> Unit)?,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onContextClick: (() -> Unit)?,
    onExitImmersive: () -> Unit,
    onEntryClick: (Entry) -> Unit,
    onLibraryAction: (Entry) -> Unit,
    showBackToTop: Boolean,
    onBackToTop: () -> Unit,
    onOpenChapter: (() -> Unit)?,
    onZoomStateChange: (Boolean) -> Unit,
    onRetry: () -> Unit,
) {
    var bottomOverlayHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val controlsBottomInset = with(density) { bottomOverlayHeightPx.toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val readyHandle = (itemState as? EntryImmersiveScreenModel.ItemState.Ready)?.handle
        if (entry != null && shouldShowImmersivePoster(readyHandle)) {
            AsyncImage(
                model = entry.asEntryCover(),
                contentDescription = entry.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
        }

        when (itemState) {
            is EntryImmersiveScreenModel.ItemState.Ready -> {
                val renderer = remember(itemState.handle, immersiveModel) {
                    immersiveModel.renderer(itemState.handle)
                }
                renderer.Content(
                    modifier = Modifier.fillMaxSize(),
                    active = isActive,
                    controlsVisible = controlsVisible,
                    controlsBottomInset = controlsBottomInset,
                    onToggleControls = onToggleControls,
                    onZoomStateChange = onZoomStateChange,
                    onProgress = { immersiveModel.persistProgress(itemState.handle, it) },
                )
            }
            is EntryImmersiveScreenModel.ItemState.Error -> ImmersiveError(
                message = itemState.throwable.message ?: stringResource(MR.strings.unknown_error),
                onRetry = onRetry,
                modifier = Modifier.align(Alignment.Center),
            )
            is EntryImmersiveScreenModel.ItemState.Loading, null -> if (isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleControls,
                        ),
                )
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(40.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.2f),
                )
            }
        }

        if (entry != null) {
            ImmersiveOverlay(
                visible = controlsVisible,
                entry = entry,
                chapterName = (itemState as? EntryImmersiveScreenModel.ItemState.Ready)?.chapter?.name,
                contextLabel = contextLabel,
                contextLeadingContent = contextLeadingContent,
                onContextClick = onContextClick,
                onExitImmersive = onExitImmersive,
                onEntryClick = { onEntryClick(entry) },
                onLibraryAction = { onLibraryAction(entry) },
                showBackToTop = showBackToTop,
                onBackToTop = onBackToTop,
                onOpenChapter = onOpenChapter,
                onBottomOverlaySize = { bottomOverlayHeightPx = it },
            )
        }
    }
}

@Composable
private fun ImmersiveOverlay(
    visible: Boolean,
    entry: Entry,
    chapterName: String?,
    contextLabel: String,
    contextLeadingContent: (@Composable () -> Unit)?,
    onContextClick: (() -> Unit)?,
    onExitImmersive: () -> Unit,
    onEntryClick: () -> Unit,
    onLibraryAction: () -> Unit,
    showBackToTop: Boolean,
    onBackToTop: () -> Unit,
    onOpenChapter: (() -> Unit)?,
    onBottomOverlaySize: (Int) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { -it / 5 },
            exit = fadeOut() + slideOutVertically { -it / 5 },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent),
                        ),
                    )
                    .padding(
                        start = 12.dp,
                        top = WindowInsets.statusBarsIgnoringVisibility
                            .asPaddingValues()
                            .calculateTopPadding() + 12.dp,
                        end = 12.dp,
                        bottom = 36.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ImmersiveContextPill(
                    label = contextLabel,
                    leadingContent = contextLeadingContent,
                    onClick = onContextClick,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { it / 5 },
            exit = fadeOut() + slideOutVertically { it / 5 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { onBottomOverlaySize(it.height) }
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)),
                        ),
                    )
                    .padding(
                        start = 16.dp,
                        top = 40.dp,
                        end = 16.dp,
                        bottom = WindowInsets.navigationBarsIgnoringVisibility
                            .asPaddingValues()
                            .calculateBottomPadding() + 8.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = entry.displayTitle,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    chapterName?.let {
                        Text(
                            text = it,
                            color = Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showBackToTop) {
                        ImmersiveActionButton(
                            label = stringResource(MR.strings.action_move_to_top),
                            icon = Icons.Outlined.KeyboardArrowUp,
                            onClick = onBackToTop,
                        )
                    }
                    ImmersiveActionButton(
                        label = stringResource(MR.strings.browse_exit_immersive),
                        icon = Icons.Outlined.FullscreenExit,
                        onClick = onExitImmersive,
                    )
                    ImmersiveActionButton(
                        label = stringResource(
                            if (entry.favorite) MR.strings.remove_from_library else MR.strings.add_to_library,
                        ),
                        icon = if (entry.favorite) Icons.Outlined.Delete else Icons.Outlined.FavoriteBorder,
                        onClick = onLibraryAction,
                    )
                    ImmersiveActionButton(
                        label = stringResource(MR.strings.action_details),
                        icon = Icons.Outlined.Info,
                        onClick = onEntryClick,
                    )
                    val presentation = entry.type.entryTypePresentation()
                    ImmersiveActionButton(
                        label = stringResource(presentation.immersiveOpenLabel),
                        icon = presentation.immersiveOpenIcon,
                        onClick = { onOpenChapter?.invoke() },
                        enabled = onOpenChapter != null,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImmersiveContextPill(
    label: String,
    leadingContent: (@Composable () -> Unit)?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingContent?.invoke()
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.52f),
            contentColor = Color.White,
            content = content,
        )
    } else {
        Surface(
            modifier = modifier,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.52f),
            contentColor = Color.White,
            content = content,
        )
    }
}

@Composable
private fun ImmersiveActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = if (enabled) 0.12f else 0.05f),
        contentColor = Color.White.copy(alpha = if (enabled) 1f else 0.38f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun ImmersiveError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onRetry) { Text(text = stringResource(MR.strings.action_retry)) }
        }
    }
}

@Composable
internal fun ImmersiveSystemBarsEffect(enabled: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current

    DisposableEffect(context, view, enabled) {
        val activity = context as? Activity
        if (activity == null || !enabled) {
            onDispose {}
        } else {
            val controller = WindowInsetsControllerCompat(activity.window, view)
            val previousBehavior = controller.systemBarsBehavior
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())

            onDispose {
                controller.systemBarsBehavior = previousBehavior
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

internal fun shouldShowImmersivePoster(handle: EntryImmersiveHandle?): Boolean = true

private const val PRELOAD_RADIUS = 1
private const val LOAD_MORE_PAGE_THRESHOLD = 3
