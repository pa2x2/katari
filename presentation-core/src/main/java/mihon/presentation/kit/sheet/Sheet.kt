package mihon.presentation.kit.sheet

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import mihon.presentation.kit.feedback.LocalScreenMessages
import mihon.presentation.kit.feedback.SnackbarHost
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.feedback.claimScreenMessages
import mihon.presentation.kit.feedback.rememberScreenMessagesFor
import mihon.presentation.kit.list.LocalRowGroupContainer
import mihon.presentation.kit.list.ProvideRowDensity
import mihon.presentation.kit.list.RowDensity
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.window.currentWindowLayout
import kotlin.math.roundToInt

/**
 * The app's bottom sheet. On phones it slides up from the bottom and is dismissed by dragging it down; on tablets it
 * shows centred, like a dialog. Its content is capped at [SheetMaxWidth] and laid out at compact row density.
 * [enableImplicitDismiss] false keeps it open on drags, outside taps and back, such as while a form has edits.
 * [capHeight] stops a long list at most of the window's height so the content behind stays visible.
 * The sheet is its own window, so messages about changes made in it show over it: from [snackbarHostState] when the
 * caller makes its own snackbars, otherwise through the sheet's ScreenMessages.
 */
@Composable
fun Sheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    enableImplicitDismiss: Boolean = true,
    capHeight: Boolean = false,
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable () -> Unit,
) {
    val isTabletUi = currentWindowLayout().usesTwoPanes
    val hostState = snackbarHostState ?: remember { SnackbarHostState() }
    val messages = rememberScreenMessagesFor(hostState)
    claimScreenMessages(messages)
    Dialog(onDismissRequest = onDismissRequest, properties = sheetDialogProperties) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            SheetLayout(
                isTabletUi = isTabletUi,
                enableImplicitDismiss = enableImplicitDismiss,
                onDismissRequest = onDismissRequest,
                modifier = if (capHeight) {
                    modifier.heightIn(
                        max = maxHeight * SHEET_CAPPED_HEIGHT_FRACTION,
                    )
                } else {
                    modifier
                },
            ) {
                CompositionLocalProvider(
                    LocalRowGroupContainer provides SurfaceContainer.Highest,
                    LocalScreenMessages provides messages,
                ) {
                    ProvideRowDensity(RowDensity.Compact, content)
                }
            }
            SnackbarHost(
                hostState = hostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime)),
            )
        }
    }
}

@Composable
private fun SheetLayout(
    isTabletUi: Boolean,
    enableImplicitDismiss: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    if (isTabletUi) {
        var targetAlpha by remember { mutableFloatStateOf(0f) }
        val alpha by animateFloatAsState(
            targetValue = targetAlpha,
            animationSpec = sheetAnimationSpec,
            label = "alpha",
        )
        val internalOnDismissRequest: () -> Unit = {
            scope.launch {
                targetAlpha = 0f
                onDismissRequest()
            }
        }
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = null,
                    indication = null,
                    enabled = enableImplicitDismiss,
                    onClick = internalOnDismissRequest,
                )
                .fillMaxSize()
                .alpha(alpha),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .requiredWidthIn(max = SheetMaxWidth)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = {},
                    )
                    .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime))
                    .padding(vertical = KatariTheme.spacing.l)
                    .then(modifier),
                shape = KatariTheme.shapes.extraLarge,
                color = KatariTheme.colors.surfaceContainerHigh,
                content = {
                    BackHandler(
                        enabled = enableImplicitDismiss && remember { derivedStateOf { alpha > 0f } }.value,
                        onBack = internalOnDismissRequest,
                    )
                    content()
                },
            )

            LaunchedEffect(Unit) {
                targetAlpha = 1f
            }
        }
    } else {
        val anchoredDraggableState = rememberSaveable(saver = AnchoredDraggableState.Saver()) {
            AnchoredDraggableState(initialValue = 1)
        }
        val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
            state = anchoredDraggableState,
            positionalThreshold = { _: Float -> with(density) { SheetDismissThreshold.toPx() } },
            animationSpec = sheetAnimationSpec,
        )
        val internalOnDismissRequest = {
            if (anchoredDraggableState.settledValue == 0) {
                scope.launch { anchoredDraggableState.animateTo(1) }
            }
        }
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = internalOnDismissRequest,
                )
                .fillMaxSize()
                .onSizeChanged {
                    val anchors = DraggableAnchors {
                        0 at 0f
                        1 at it.height.toFloat()
                    }
                    anchoredDraggableState.updateAnchors(anchors)
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = SheetMaxWidth)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = {},
                    )
                    .then(
                        if (enableImplicitDismiss) {
                            Modifier.nestedScroll(
                                remember(anchoredDraggableState) {
                                    anchoredDraggableState.preUpPostDownNestedScrollConnection {
                                        scope.launch { anchoredDraggableState.settle(sheetAnimationSpec) }
                                    }
                                },
                            )
                        } else {
                            Modifier
                        },
                    )
                    .then(modifier)
                    .offset {
                        IntOffset(
                            0,
                            anchoredDraggableState.offset
                                .takeIf { it.isFinite() }
                                ?.roundToInt()
                                ?: 0,
                        )
                    }
                    .anchoredDraggable(
                        state = anchoredDraggableState,
                        orientation = Orientation.Vertical,
                        enabled = enableImplicitDismiss,
                        flingBehavior = flingBehavior,
                    )
                    .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime)),
                shape = KatariTheme.shapes.extraLarge,
                color = KatariTheme.colors.surfaceContainerHigh,
                content = {
                    BackHandler(
                        enabled = enableImplicitDismiss && anchoredDraggableState.targetValue == 0,
                        onBack = internalOnDismissRequest,
                    )
                    content()
                },
            )

            LaunchedEffect(anchoredDraggableState) {
                scope.launch { anchoredDraggableState.animateTo(0) }
                snapshotFlow { anchoredDraggableState.settledValue }
                    .drop(1)
                    .filter { it == 1 }
                    .collectLatest {
                        onDismissRequest()
                    }
            }
        }
    }
}

private fun <T> AnchoredDraggableState<T>.preUpPostDownNestedScrollConnection(
    onFling: (velocity: Float) -> Unit,
) = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val delta = available.toFloat()
        return if (delta < 0 && source == NestedScrollSource.UserInput) {
            dispatchRawDelta(delta).toOffset()
        } else {
            Offset.Zero
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        return if (source == NestedScrollSource.UserInput) {
            dispatchRawDelta(available.toFloat()).toOffset()
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val toFling = available.toFloat()
        return if (toFling < 0 && offset > anchors.minPosition()) {
            onFling(toFling)
            // since we go to the anchor with tween settling, consume all for the best UX
            available
        } else {
            Velocity.Zero
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        onFling(available.toFloat())
        return if (targetValue != settledValue) {
            available
        } else {
            Velocity.Zero
        }
    }

    private fun Float.toOffset(): Offset = Offset(0f, this)

    @JvmName("velocityToFloat")
    private fun Velocity.toFloat() = this.y

    @JvmName("offsetToFloat")
    private fun Offset.toFloat(): Float = this.y
}

private val sheetAnimationSpec = tween<Float>(durationMillis = 350)

val SheetMaxWidth = 460.dp

/** How far a sheet must be dragged down before letting go dismisses it. */
private val SheetDismissThreshold = 56.dp

private const val SHEET_CAPPED_HEIGHT_FRACTION = 0.85f

// The sheet owns window insets, including the IME. Letting the platform fit them would leave the keyboard to pan
// the window based on where the focused field was when it appeared, which misplaces sheets that request text focus
// while still sliding in.
private val sheetDialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = false,
)
