package mihon.translation.ui.presentation

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.launch
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.ui.session.TranslationSessionController
import mihon.translation.ui.session.TranslationSessionState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.AdaptiveSheet
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

/**
 * Full-viewport Translation overlay.
 *
 * Selection anchors must use the same root-coordinate space as this host. Empty space around an anchored popup has no
 * pointer handler, so reader interaction remains non-modal.
 */
@Composable
fun TranslationSessionHost(
    controller: TranslationSessionController,
    isTabletUi: Boolean,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = controller::dismiss,
    onPopupBoundsChanged: (Rect?) -> Unit = {},
    speechState: TranslationResultSpeechState = TranslationResultSpeechState(),
    onSpeechToggle: ((TranslationResultSpeechTarget) -> Unit)? = null,
) {
    val state by controller.state.collectAsState()
    val active = state as? TranslationSessionState.Active
    var expanded by remember(active?.input?.request) { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val clipboardLabel = stringResource(MR.strings.translation_title)

    DisposableEffect(controller) {
        onDispose(onDismiss)
    }

    TranslationSessionOverlay(
        state = state,
        expanded = expanded,
        isTabletUi = isTabletUi,
        onDismiss = onDismiss,
        onExecute = controller::execute,
        onRetry = controller::retry,
        onCopy = { text ->
            scope.launch {
                clipboard.setClipEntry(
                    ClipData.newPlainText(clipboardLabel, text).toClipEntry(),
                )
            }
        },
        onExpand = { expanded = true },
        onSelectSource = controller::selectSourceLanguage,
        onSelectEngine = controller::selectEngine,
        onExternalAction = onExternalAction,
        speechState = speechState,
        onSpeechToggle = onSpeechToggle,
        onPopupBoundsChanged = onPopupBoundsChanged,
        modifier = modifier,
    )
}

@Composable
internal fun TranslationSessionOverlay(
    state: TranslationSessionState,
    expanded: Boolean,
    isTabletUi: Boolean,
    onDismiss: () -> Unit,
    onExecute: () -> Unit,
    onRetry: () -> Unit,
    onCopy: (String) -> Unit,
    onExpand: () -> Unit,
    onSelectSource: (LanguageTag) -> Unit,
    onSelectEngine: (TranslationEngineSelection) -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    speechState: TranslationResultSpeechState = TranslationResultSpeechState(),
    onSpeechToggle: ((TranslationResultSpeechTarget) -> Unit)? = null,
    onPopupBoundsChanged: (Rect?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val currentOnPopupBoundsChanged = rememberUpdatedState(onPopupBoundsChanged)
    val active = state as? TranslationSessionState.Active
    if (active == null) {
        SideEffect { onPopupBoundsChanged(null) }
        return
    }
    val preferredSurface = if (expanded) {
        TranslationSessionSurface.AdaptiveSheet
    } else {
        state.preferredSurface()
    }
    if (preferredSurface == TranslationSessionSurface.None) return

    @Composable
    fun Sheet() {
        TranslationSessionSheetDialog(
            state = active,
            isTabletUi = isTabletUi,
            onDismiss = onDismiss,
            onExecute = onExecute,
            onRetry = onRetry,
            onCopy = onCopy,
            onExpand = onExpand,
            onSelectSource = onSelectSource,
            onSelectEngine = onSelectEngine,
            onExternalAction = onExternalAction,
            speechState = speechState,
            onSpeechToggle = onSpeechToggle,
        )
    }

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val leftInset = WindowInsets.safeDrawing.getLeft(density, layoutDirection)
    val topInset = WindowInsets.safeDrawing.getTop(density)
    val rightInset = WindowInsets.safeDrawing.getRight(density, layoutDirection)
    val bottomInset = WindowInsets.safeDrawing.getBottom(density)
    val edgeMargin = with(density) { POPUP_EDGE_MARGIN.roundToPx() }
    val anchorGap = with(density) { POPUP_ANCHOR_GAP.roundToPx() }
    val popupMaximumWidth = with(density) { POPUP_MAXIMUM_WIDTH.roundToPx() }

    val anchor = active.input.anchor
    if (preferredSurface == TranslationSessionSurface.AdaptiveSheet || anchor == null) {
        SideEffect { onPopupBoundsChanged(null) }
        Sheet()
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val hostSize = IntSize(constraints.maxWidth, constraints.maxHeight)
        val safeWidth = hostSize.width - leftInset - rightInset - edgeMargin * 2
        val safeHeight = hostSize.height - topInset - bottomInset - edgeMargin * 2
        if (safeWidth <= 0 || safeHeight <= 0) {
            SideEffect { onPopupBoundsChanged(null) }
            Sheet()
            return@BoxWithConstraints
        }
        // Anchor coordinates change on every scroll frame. Keep placement visibility scoped to the
        // translation request so the popup can pin to the viewport edge and re-anchor without
        // restarting its session.
        var placementAvailability by remember(
            active.input.request,
            hostSize,
            leftInset,
            topInset,
            rightInset,
            bottomInset,
            edgeMargin,
            anchorGap,
        ) {
            mutableStateOf<TranslationPopupPlacementAvailability?>(null)
        }
        when (placementAvailability) {
            TranslationPopupPlacementAvailability.NeedsSheet -> {
                Sheet()
                return@BoxWithConstraints
            }
            TranslationPopupPlacementAvailability.AnchorOutsideViewport,
            TranslationPopupPlacementAvailability.Fits,
            null,
            -> Unit
        }

        val positionProvider = remember(
            active.input.request,
            anchor,
            hostSize,
            leftInset,
            topInset,
            rightInset,
            bottomInset,
            edgeMargin,
            anchorGap,
        ) {
            TranslationPopupPositionProvider(
                anchor = anchor,
                hostSize = hostSize,
                windowInsets = TranslationWindowInsets(
                    left = leftInset,
                    top = topInset,
                    right = rightInset,
                    bottom = bottomInset,
                ),
                edgeMargin = edgeMargin,
                anchorGap = anchorGap,
                onPlacementAvailabilityChanged = { availability ->
                    if (placementAvailability != availability) {
                        placementAvailability = availability
                    }
                },
                onPopupBoundsChanged = { bounds -> currentOnPopupBoundsChanged.value(bounds) },
            )
        }

        Popup(
            popupPositionProvider = positionProvider,
            onDismissRequest = onDismiss,
            properties = translationPopupProperties,
        ) {
            TranslationSessionPopup(
                state = active,
                maximumWidth = with(density) {
                    minOf(popupMaximumWidth, safeWidth).toDp()
                },
                visible = placementAvailability == TranslationPopupPlacementAvailability.Fits,
                onDismiss = onDismiss,
                onExecute = onExecute,
                onRetry = onRetry,
                onCopy = onCopy,
                onExpand = onExpand,
                onSelectSource = onSelectSource,
                onSelectEngine = onSelectEngine,
                onExternalAction = onExternalAction,
                speechState = speechState,
                onSpeechToggle = onSpeechToggle,
            )
        }
    }
}

@Composable
private fun TranslationSessionSheetDialog(
    state: TranslationSessionState.Active,
    isTabletUi: Boolean,
    onDismiss: () -> Unit,
    onExecute: () -> Unit,
    onRetry: () -> Unit,
    onCopy: (String) -> Unit,
    onExpand: () -> Unit,
    onSelectSource: (LanguageTag) -> Unit,
    onSelectEngine: (TranslationEngineSelection) -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    speechState: TranslationResultSpeechState,
    onSpeechToggle: ((TranslationResultSpeechTarget) -> Unit)?,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = translationSessionDialogProperties,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AdaptiveSheet(
                isTabletUi = isTabletUi,
                enableImplicitDismiss = true,
                onDismissRequest = onDismiss,
                modifier = Modifier.testTag(TRANSLATION_SESSION_SHEET_TAG),
            ) {
                TranslationSessionContent(
                    state = state,
                    expanded = true,
                    showHeader = true,
                    showExpand = false,
                    showLanguageChange = true,
                    showEngineChange = true,
                    showCopy = true,
                    useExternalEnginePicker = false,
                    onDismiss = onDismiss,
                    onExecute = onExecute,
                    onRetry = onRetry,
                    onCopy = onCopy,
                    onExpand = onExpand,
                    onSelectSource = onSelectSource,
                    onSelectEngine = onSelectEngine,
                    onExternalAction = onExternalAction,
                    speechState = speechState,
                    onSpeechToggle = onSpeechToggle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}

@Composable
private fun TranslationSessionPopup(
    state: TranslationSessionState.Active,
    maximumWidth: Dp,
    visible: Boolean,
    onDismiss: () -> Unit,
    onExecute: () -> Unit,
    onRetry: () -> Unit,
    onCopy: (String) -> Unit,
    onExpand: () -> Unit,
    onSelectSource: (LanguageTag) -> Unit,
    onSelectEngine: (TranslationEngineSelection) -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    speechState: TranslationResultSpeechState,
    onSpeechToggle: ((TranslationResultSpeechTarget) -> Unit)?,
) {
    BackHandler(onBack = onDismiss)
    Box(modifier = Modifier.alpha(if (visible) 1f else 0f)) {
        Surface(
            modifier = Modifier
                .widthIn(max = maximumWidth)
                .then(
                    if (visible) Modifier else Modifier.semantics { hideFromAccessibility() },
                )
                .testTag(TRANSLATION_SESSION_POPUP_TAG),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            TranslationSessionContent(
                state = state,
                expanded = false,
                showHeader = true,
                showExpand = true,
                showLanguageChange = true,
                showEngineChange = true,
                showCopy = true,
                useExternalEnginePicker = false,
                onDismiss = onDismiss,
                onExecute = onExecute,
                onRetry = onRetry,
                onCopy = onCopy,
                onExpand = onExpand,
                onSelectSource = onSelectSource,
                onSelectEngine = onSelectEngine,
                onExternalAction = onExternalAction,
                speechState = speechState,
                onSpeechToggle = onSpeechToggle,
                compact = true,
            )
        }
    }
}

internal data class TranslationWindowInsets(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

internal enum class TranslationPopupPlacementAvailability {
    Fits,
    AnchorOutsideViewport,
    NeedsSheet,
}

internal class TranslationPopupPositionProvider(
    private val anchor: mihon.translation.ui.session.TranslationSelectionAnchor,
    private val hostSize: IntSize,
    private val windowInsets: TranslationWindowInsets,
    private val edgeMargin: Int,
    private val anchorGap: Int,
    private val onPlacementAvailabilityChanged: (TranslationPopupPlacementAvailability) -> Unit,
    private val onPopupBoundsChanged: (Rect?) -> Unit = {},
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val rootLeft = anchorBounds.left
        val rootTop = anchorBounds.top
        val viewportLeft = (windowInsets.left - rootLeft).coerceAtLeast(0)
        val viewportTop = (windowInsets.top - rootTop).coerceAtLeast(0)
        val viewportRight = minOf(
            hostSize.width,
            windowSize.width - rootLeft - windowInsets.right,
        )
        val viewportBottom = minOf(
            hostSize.height,
            windowSize.height - rootTop - windowInsets.bottom,
        )
        val viewport = if (viewportRight > viewportLeft && viewportBottom > viewportTop) {
            TranslationViewportBounds(
                left = viewportLeft,
                top = viewportTop,
                right = viewportRight,
                bottom = viewportBottom,
            )
        } else {
            null
        }
        val placement = if (viewport != null) {
            calculateTranslationPopupPlacement(
                anchor = anchor,
                popup = TranslationPopupSize(popupContentSize.width, popupContentSize.height),
                viewport = viewport,
                edgeMargin = edgeMargin,
                anchorGap = anchorGap,
            )
        } else {
            null
        }
        val availability = when {
            placement != null -> TranslationPopupPlacementAvailability.Fits
            viewport != null && anchor.isUsable() &&
                !anchor.isInside(viewport, edgeMargin = 0) ->
                TranslationPopupPlacementAvailability.AnchorOutsideViewport
            else -> TranslationPopupPlacementAvailability.NeedsSheet
        }
        val resolvedPosition = placement?.let {
            IntOffset(rootLeft + it.x, rootTop + it.y)
        } ?: calculateTranslationPopupFallbackPosition(
            anchor = anchor,
            popupContentSize = popupContentSize,
            viewportLeft = viewportLeft,
            viewportTop = viewportTop,
            viewportRight = viewportRight,
            viewportBottom = viewportBottom,
            rootLeft = rootLeft,
            rootTop = rootTop,
            edgeMargin = edgeMargin,
            anchorGap = anchorGap,
        )
        onPlacementAvailabilityChanged(availability)
        onPopupBoundsChanged(
            resolvedPosition.takeIf {
                availability != TranslationPopupPlacementAvailability.NeedsSheet
            }?.let {
                Rect(
                    left = it.x.toFloat(),
                    top = it.y.toFloat(),
                    right = (it.x + popupContentSize.width).toFloat(),
                    bottom = (it.y + popupContentSize.height).toFloat(),
                )
            },
        )
        return resolvedPosition
    }
}

private fun calculateTranslationPopupFallbackPosition(
    anchor: mihon.translation.ui.session.TranslationSelectionAnchor,
    popupContentSize: IntSize,
    viewportLeft: Int,
    viewportTop: Int,
    viewportRight: Int,
    viewportBottom: Int,
    rootLeft: Int,
    rootTop: Int,
    edgeMargin: Int,
    anchorGap: Int,
): IntOffset {
    val safeLeft = (viewportLeft + edgeMargin).coerceAtMost(viewportRight)
    val safeTop = (viewportTop + edgeMargin).coerceAtMost(viewportBottom)
    val safeRight = (viewportRight - edgeMargin).coerceAtLeast(safeLeft)
    val safeBottom = (viewportBottom - edgeMargin).coerceAtLeast(safeTop)
    val maximumX = (safeRight - popupContentSize.width).coerceAtLeast(safeLeft)
    val maximumY = (safeBottom - popupContentSize.height).coerceAtLeast(safeTop)
    val anchorCenterX = ((anchor.left + anchor.right) / 2f)
        .takeIf(Float::isFinite)
        ?.roundToInt()
        ?: safeLeft
    val preferredX = anchorCenterX - popupContentSize.width / 2
    val preferredY = anchor.top
        .takeIf(Float::isFinite)
        ?.roundToInt()
        ?.minus(anchorGap + popupContentSize.height)
        ?: safeTop
    return IntOffset(
        x = rootLeft + preferredX.coerceIn(safeLeft, maximumX),
        y = rootTop + preferredY.coerceIn(safeTop, maximumY),
    )
}

private val POPUP_EDGE_MARGIN = 16.dp

// Text selection handles extend below the selected glyph bounds and receive a 36 dp minimum
// touch target. Keep popup actions clear of that full target so the handle cannot consume taps
// intended for the popup.
private val POPUP_ANCHOR_GAP = 36.dp
private val POPUP_MAXIMUM_WIDTH = 360.dp
private val translationPopupProperties = PopupProperties(
    focusable = false,
    dismissOnBackPress = false,
    dismissOnClickOutside = false,
    clippingEnabled = false,
)

internal const val TRANSLATION_SESSION_POPUP_TAG = "translation_session_popup"
internal const val TRANSLATION_SESSION_SHEET_TAG = "translation_session_sheet"
