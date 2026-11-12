package app.katari.presentation.kit.sheet

import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.katari.presentation.kit.dialog.DialogBackHandler
import app.katari.presentation.kit.dialog.DialogDim
import app.katari.presentation.kit.dialog.DialogWindowDim
import app.katari.presentation.kit.dialog.OutsideTapArea
import app.katari.presentation.kit.dialog.kitDialogProperties
import app.katari.presentation.kit.feedback.LocalScreenMessages
import app.katari.presentation.kit.feedback.ScreenSnackbarHost
import app.katari.presentation.kit.feedback.SnackbarHostState
import app.katari.presentation.kit.feedback.claimScreenMessages
import app.katari.presentation.kit.feedback.rememberScreenMessagesFor
import app.katari.presentation.kit.layout.ApartFromScreen
import app.katari.presentation.kit.list.LocalRowGroupContainer
import app.katari.presentation.kit.list.ProvideRowDensity
import app.katari.presentation.kit.list.RowDensity
import app.katari.presentation.kit.navigation.LocalDockOnScreen
import app.katari.presentation.kit.pointer.onWheelScrollEnd
import app.katari.presentation.kit.shortcut.KeyboardShortcutLayer
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.window.FoldHalf
import app.katari.presentation.kit.window.LocalLastPress
import app.katari.presentation.kit.window.WindowWidth
import app.katari.presentation.kit.window.currentWindowLayout
import app.katari.presentation.kit.window.rememberFoldHalf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The app's sheet. In a compact window it slides up from the bottom and is dismissed by dragging it down, its content
 * capped at [SheetMaxWidth]; from a medium window on it is a full-height panel at the side of the window it was opened
 * from (see [sheetEdge]), beside the dock on the dock's side, dismissed by dragging it back. While a vertical fold is
 * half-open it stays within one half (see [rememberFoldHalf]). In a large window the side sheet is wide enough for two
 * columns of rows, and its content fills it: row groups and the lists of [RowList] wrap their rows into both, a
 * [SheetSplitList] sets its two parts side by side, and text, cards and tiles take the whole width.
 * Its content is laid out at compact row density.
 * A side sheet's title row ([SheetHeader], a [TabbedSheet]'s tabs) ends with a close button.
 * [enableImplicitDismiss] false keeps it open on drags, outside taps and back, and leaves out the close button, such as
 * while a form has edits.
 * [capHeight] stops a long list at most of the window's height so the content behind stays visible.
 * [backdrop] says whether the screen behind is dimmed.
 * The sheet is its own window, so messages about changes made in it show over it: the snackbars the caller makes in
 * [snackbarHostState] and the sheet's ScreenMessages, one at a time as on a Scaffold.
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
    val window = currentWindowLayout()
    val half = rememberFoldHalf()
    val lastPress = LocalLastPress.current
    val density = LocalDensity.current
    val edge = remember(window.usesSideNavigation, half) {
        sheetEdge(
            sideSheets = window.usesSideNavigation,
            half = half,
            windowWidth = window.size.width,
            pressX = lastPress?.openingPositionInWindow?.let { with(density) { it.x.toDp() } },
        )
    }
    val dockRoom = dockRoomBeside(edge)
    val twoColumns = edge != SheetEdge.Bottom && half == null && window.width >= WindowWidth.Large
    val messages = rememberScreenMessagesFor(remember { SnackbarHostState() })
    val messagesHost = claimScreenMessages(messages)
    val dim = when {
        backdrop == SheetBackdrop.Clear -> DialogDim.None
        // A bottom sheet covers most of what it acts on, so its dim hides little
        backdrop == SheetBackdrop.Light && edge != SheetEdge.Bottom -> DialogDim.Light
        else -> DialogDim.Full
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        // The sheet owns window insets, including the IME. Letting the platform fit them would leave the keyboard to
        // pan the window based on where the focused field was when it appeared, which misplaces sheets that request
        // text focus while still sliding in.
        properties = kitDialogProperties(fitsSystemWindows = false, dim = dim),
    ) {
        DialogWindowDim(dim)
        BoxWithConstraints(Modifier.fillMaxSize()) {
            SheetLayout(
                edge = edge,
                half = half,
                dockRoom = dockRoom,
                sideWidth = if (twoColumns) TwoColumnSheetWidth else SideSheetWidth,
                snackbarHostState = snackbarHostState,
                messagesHost = messagesHost,
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
                // The sheet is a window over the floating dock and the screen, so a Scaffold or list in it has nothing
                // to clear
                ApartFromScreen {
                    CompositionLocalProvider(
                        LocalRowGroupContainer provides SurfaceContainer.Highest,
                        LocalScreenMessages provides messages,
                        LocalSheetForm provides if (edge == SheetEdge.Bottom) SheetForm.Bottom else SheetForm.Side,
                        LocalSheetColumns provides if (twoColumns) 2 else 1,
                    ) {
                        KeyboardShortcutLayer {
                            ProvideRowDensity(RowDensity.Compact, content)
                        }
                    }
                }
            }
        }
    }
}

/** What a [Sheet], or a viewer's side panel, does to the screen behind it. */
enum class SheetBackdrop {
    /** Dimmed, setting the sheet apart from what it covers. */
    Dimmed,

    /**
     * Lightly dimmed beside a side sheet, for a sheet whose changes apply at once to what it covers, such as a list's
     * filters, so the list stays readable; dimmed in full under a bottom sheet, which covers most of it anyway.
     */
    Light,

    /** Left as it is, for a sheet whose changes show behind it as they are made, such as a reader's appearance. */
    Clear,
}

/**
 * How many columns of rows the sheet around the content holds: 2 in a large window's side sheet, where row groups wrap
 * their rows into both; 1 elsewhere, and inside anything that keeps its rows to one column, such as a part of a
 * [SheetSplitList] or a dialog opened from the sheet.
 */
internal val LocalSheetColumns = staticCompositionLocalOf { 1 }

/** The form an open sheet's content is shown in, for the parts that differ, such as a side sheet's close button. */
internal enum class SheetForm { Bottom, Side, Popover }

/** The form of the sheet or [Popover] around the content, or null outside one. */
internal val LocalSheetForm = staticCompositionLocalOf<SheetForm?> { null }

/** The window edge a sheet slides in from and is dragged back to, as seen on screen whatever the layout direction. */
internal enum class SheetEdge { Bottom, Left, Right }

/**
 * Where a sheet opens: from the bottom without [sideSheets]; otherwise at the side of the window, or of the fold's
 * [half], that the press opening it landed in ([pressX], null when it was opened from the keyboard or by nothing the
 * user pressed), so the pointer or hand that opened it doesn't cross the window to reach it. With no press it opens at
 * the right edge.
 */
internal fun sheetEdge(sideSheets: Boolean, half: FoldHalf?, windowWidth: Dp, pressX: Dp?): SheetEdge {
    if (!sideSheets) return SheetEdge.Bottom
    val left = half?.left ?: 0.dp
    val right = half?.right ?: windowWidth
    return if (pressX != null && pressX < (left + right) / 2) SheetEdge.Left else SheetEdge.Right
}

/** The room the side dock takes at [edge], so a sheet there opens beside the dock rather than over it. */
@Composable
private fun dockRoomBeside(edge: SheetEdge): Dp {
    val dockStart = LocalDockOnScreen.current.start
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    return when (edge) {
        SheetEdge.Left -> if (rtl) 0.dp else dockStart
        SheetEdge.Right -> if (rtl) dockStart else 0.dp
        SheetEdge.Bottom -> 0.dp
    }
}

@Composable
private fun SheetLayout(
    edge: SheetEdge,
    half: FoldHalf?,
    dockRoom: Dp,
    sideWidth: Dp,
    snackbarHostState: SnackbarHostState?,
    messagesHost: SnackbarHostState?,
    enableImplicitDismiss: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val anchoredDraggableState = rememberSaveable(saver = AnchoredDraggableState.Saver()) {
        AnchoredDraggableState(initialValue = 1)
    }
    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = anchoredDraggableState,
        positionalThreshold = { _: Float -> with(density) { SheetDismissThreshold.toPx() } },
        animationSpec = sheetAnimationSpec,
    )
    val internalOnDismissRequest: () -> Unit = {
        if (anchoredDraggableState.settledValue == 0) {
            scope.launch { anchoredDraggableState.animateTo(1) }
        }
    }
    val insets = WindowInsets.systemBars.union(WindowInsets.ime)
    Box(modifier = Modifier.fillMaxSize()) {
        OutsideTapArea(enabled = enableImplicitDismiss, onTap = internalOnDismissRequest)
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
            contentAlignment = when (edge) {
                SheetEdge.Bottom -> Alignment.BottomCenter
                SheetEdge.Left -> AbsoluteAlignment.CenterLeft
                SheetEdge.Right -> AbsoluteAlignment.CenterRight
            },
        ) {
            val formModifier = when (edge) {
                SheetEdge.Bottom -> Modifier.widthIn(max = SheetMaxWidth)
                SheetEdge.Left, SheetEdge.Right ->
                    Modifier
                        .fillMaxHeight()
                        .absolutePadding(
                            left = if (edge == SheetEdge.Left) dockRoom else 0.dp,
                            right = if (edge == SheetEdge.Right) dockRoom else 0.dp,
                        )
                        .width(minOf(sideWidth, half?.width ?: sideWidth))
                        .windowInsetsPadding(insets.union(WindowInsets.displayCutout))
                        .padding(KatariTheme.spacing.m)
            }
            Surface(
                modifier = formModifier
                    .then(
                        if (enableImplicitDismiss && edge == SheetEdge.Bottom) {
                            Modifier
                                .onWheelScrollEnd { anchoredDraggableState.settle(sheetAnimationSpec) }
                                .nestedScroll(
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
                            SheetEdge.Left -> IntOffset(-offset, 0)
                            SheetEdge.Right -> IntOffset(offset, 0)
                        }
                    }
                    .anchoredDraggable(
                        state = anchoredDraggableState,
                        orientation = if (edge == SheetEdge.Bottom) Orientation.Vertical else Orientation.Horizontal,
                        enabled = enableImplicitDismiss,
                        reverseDirection = edge == SheetEdge.Left,
                        flingBehavior = flingBehavior,
                    )
                    .then(if (edge == SheetEdge.Bottom) Modifier.windowInsetsPadding(insets) else Modifier),
                shape = KatariTheme.shapes.extraLarge,
                color = KatariTheme.colors.surfaceContainerHigh,
                content = {
                    DialogBackHandler(
                        enabled = enableImplicitDismiss && anchoredDraggableState.targetValue == 0,
                        onBack = internalOnDismissRequest,
                    )
                    CompositionLocalProvider(
                        LocalSheetClose provides internalOnDismissRequest.takeIf {
                            enableImplicitDismiss && edge != SheetEdge.Bottom
                        },
                        content = content,
                    )
                },
            )
            ScreenSnackbarHost(
                own = snackbarHostState,
                messages = messagesHost,
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

// Two columns of rows, each nearly as wide as a one-column sheet's
private val TwoColumnSheetWidth = 760.dp

/** How far a sheet must be dragged down before letting go dismisses it. */
private val SheetDismissThreshold = 56.dp

private const val SHEET_CAPPED_HEIGHT_FRACTION = 0.85f
