package mihon.presentation.kit.sheet

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
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
import mihon.presentation.kit.navigation.DockClearance
import mihon.presentation.kit.navigation.LocalDockClearance
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.window.FoldHalf
import mihon.presentation.kit.window.currentWindowLayout
import mihon.presentation.kit.window.rememberFoldHalf
import kotlin.math.roundToInt

/**
 * The app's sheet. In a narrow window it slides up from the bottom and is dismissed by dragging it down, its content
 * capped at [SheetMaxWidth]; in a wide one it is a full-height panel at the end edge, beside the content it acts on,
 * dismissed by dragging it back. While a vertical fold is half-open it stays within one half (see [rememberFoldHalf]).
 * Its content is laid out at compact row density.
 * [enableImplicitDismiss] false keeps it open on drags, outside taps and back, such as while a form has edits.
 * [capHeight] stops a long list at most of the window's height so the content behind stays visible.
 * [backdrop] says whether the screen behind is dimmed.
 * The sheet is its own window, so messages about changes made in it show over it: from [snackbarHostState] when the
 * caller makes its own snackbars, otherwise through the sheet's ScreenMessages.
 */
@Composable
fun Sheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    enableImplicitDismiss: Boolean = true,
    capHeight: Boolean = false,
    backdrop: SheetBackdrop = SheetBackdrop.Dimmed,
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable () -> Unit,
) {
    val edge = if (currentWindowLayout().usesTwoPanes) SheetEdge.End else SheetEdge.Bottom
    val half = rememberFoldHalf()
    val hostState = snackbarHostState ?: remember { SnackbarHostState() }
    val messages = rememberScreenMessagesFor(hostState)
    claimScreenMessages(messages)
    Dialog(onDismissRequest = onDismissRequest, properties = sheetDialogProperties) {
        if (backdrop == SheetBackdrop.Clear) {
            // The platform dims behind every dialog window; only the window itself can take that back
            val view = LocalView.current
            SideEffect { (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f) }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            SheetLayout(
                edge = edge,
                half = half,
                hostState = hostState,
                enableImplicitDismiss = enableImplicitDismiss,
                onDismissRequest = onDismissRequest,
                modifier = if (capHeight && edge == SheetEdge.Bottom) {
                    modifier.heightIn(
                        max = maxHeight * SHEET_CAPPED_HEIGHT_FRACTION,
                    )
                } else {
                    modifier
                },
            ) {
                // The sheet is a window over the floating dock, so a Scaffold in it has nothing to clear
                CompositionLocalProvider(
                    LocalRowGroupContainer provides SurfaceContainer.Highest,
                    LocalScreenMessages provides messages,
                    LocalDockClearance provides DockClearance.None,
                ) {
                    ProvideRowDensity(RowDensity.Compact, content)
                }
            }
        }
    }
}

/** What a [Sheet] does to the screen behind it. */
enum class SheetBackdrop {
    /** Dimmed, setting the sheet apart from what it covers. */
    Dimmed,

    /** Left as it is, for a sheet whose changes show behind it as they are made, such as a reader's appearance. */
    Clear,
}

/** The window edge a sheet slides in from and is dragged back to. */
private enum class SheetEdge { Bottom, End }

@Composable
private fun SheetLayout(
    edge: SheetEdge,
    half: FoldHalf?,
    hostState: SnackbarHostState,
    enableImplicitDismiss: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
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
    val insets = WindowInsets.systemBars.union(WindowInsets.ime)
    Box(
        modifier = Modifier
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = enableImplicitDismiss,
                onClick = internalOnDismissRequest,
            )
            .fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .inHalf(half)
                .onSizeChanged {
                    val anchors = DraggableAnchors {
                        0 at 0f
                        1 at (if (edge == SheetEdge.Bottom) it.height else it.width).toFloat()
                    }
                    anchoredDraggableState.updateAnchors(anchors)
                },
            contentAlignment = if (edge == SheetEdge.Bottom) Alignment.BottomCenter else Alignment.CenterEnd,
        ) {
            val formModifier = when (edge) {
                SheetEdge.Bottom -> Modifier.widthIn(max = SheetMaxWidth)
                SheetEdge.End ->
                    Modifier
                        .fillMaxHeight()
                        .width(minOf(SideSheetWidth, half?.width ?: SideSheetWidth))
                        .windowInsetsPadding(insets.union(WindowInsets.displayCutout))
                        .padding(KatariTheme.spacing.m)
            }
            Surface(
                modifier = formModifier
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = {},
                    )
                    .then(
                        if (enableImplicitDismiss && edge == SheetEdge.Bottom) {
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
                        val offset = anchoredDraggableState.offset.takeIf { it.isFinite() }?.roundToInt() ?: 0
                        when (edge) {
                            SheetEdge.Bottom -> IntOffset(0, offset)
                            SheetEdge.End -> IntOffset(if (isRtl) -offset else offset, 0)
                        }
                    }
                    .anchoredDraggable(
                        state = anchoredDraggableState,
                        orientation = if (edge == SheetEdge.Bottom) Orientation.Vertical else Orientation.Horizontal,
                        enabled = enableImplicitDismiss,
                        reverseDirection = edge == SheetEdge.End && isRtl,
                        flingBehavior = flingBehavior,
                    )
                    .then(if (edge == SheetEdge.Bottom) Modifier.windowInsetsPadding(insets) else Modifier),
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
            SnackbarHost(
                hostState = hostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(insets),
            )
        }

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

/** Confines a window-wide surface to [half] of a half-open fold, or lets it fill the window. */
private fun Modifier.inHalf(half: FoldHalf?): Modifier = if (half == null) {
    fillMaxSize()
} else {
    fillMaxHeight()
        .absoluteOffset(x = half.left)
        .width(half.width)
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

private val SideSheetWidth = 400.dp

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
