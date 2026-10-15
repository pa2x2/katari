package mihon.translation.ui.presentation

import android.content.ClipData
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.roundToIntRect
import kotlinx.coroutines.launch
import mihon.language.api.tag.LanguageTag
import mihon.presentation.kit.theme.KatariTheme
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.ui.session.TranslationSessionController
import mihon.translation.ui.session.TranslationSessionState
import mihon.translation.ui.session.language.TranslationLanguageSuggestions
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Full-viewport Translation overlay.
 *
 * Selection anchors must use the same root-coordinate space as this host. Empty space around an anchored popup has no
 * pointer handler, so reader interaction remains non-modal. [popupAvoids] is reader chrome drawn over the content in
 * that space, such as a floating toolbar, which the popup must not cover.
 */
@Composable
fun TranslationSessionHost(
    controller: TranslationSessionController,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = controller::dismiss,
    onPopupBoundsChanged: (Rect?) -> Unit = {},
    popupAvoids: Rect? = null,
    speechState: TranslationResultSpeechState = TranslationResultSpeechState(),
    onSpeechToggle: ((TranslationResultSpeechTarget) -> Unit)? = null,
    onSelectSource: (LanguageTag) -> Unit = controller::selectSourceLanguage,
    onSelectTarget: (LanguageTag) -> Unit = controller::selectTargetLanguage,
    onSelectEngine: (TranslationEngineSelection) -> Unit = controller::selectEngine,
    languageSuggestions: TranslationLanguageSuggestions? = null,
) {
    val state by controller.state.collectAsState()
    val active = state as? TranslationSessionState.Active
    // Picking a language or engine replaces the request, so fullscreen follows the selected text to survive it.
    var expanded by remember(active?.input?.request?.text) { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val clipboardLabel = stringResource(MR.strings.translation_title)

    DisposableEffect(controller) {
        onDispose(onDismiss)
    }

    TranslationSessionOverlay(
        state = state,
        expanded = expanded,
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
        onCollapse = { expanded = false },
        onSelectSource = onSelectSource,
        onSelectTarget = onSelectTarget,
        onSelectEngine = onSelectEngine,
        onExternalAction = onExternalAction,
        languageSuggestions = languageSuggestions,
        speechState = speechState,
        onSpeechToggle = onSpeechToggle,
        onPopupBoundsChanged = onPopupBoundsChanged,
        popupAvoids = popupAvoids,
        modifier = modifier,
    )
}

@Composable
internal fun TranslationSessionOverlay(
    state: TranslationSessionState,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onExecute: () -> Unit,
    onRetry: () -> Unit,
    onCopy: (String) -> Unit,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onSelectSource: (LanguageTag) -> Unit,
    onSelectEngine: (TranslationEngineSelection) -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    onSelectTarget: (LanguageTag) -> Unit = {},
    languageSuggestions: TranslationLanguageSuggestions? = null,
    speechState: TranslationResultSpeechState = TranslationResultSpeechState(),
    onSpeechToggle: ((TranslationResultSpeechTarget) -> Unit)? = null,
    onPopupBoundsChanged: (Rect?) -> Unit = {},
    popupAvoids: Rect? = null,
    modifier: Modifier = Modifier,
) {
    val currentOnPopupBoundsChanged = rememberUpdatedState(onPopupBoundsChanged)
    val active = state as? TranslationSessionState.Active
    if (active == null) {
        SideEffect { onPopupBoundsChanged(null) }
        return
    }
    val collapsedSurface = state.preferredSurface()
    val preferredSurface = if (expanded) TranslationSessionSurface.AdaptiveSheet else collapsedSurface
    if (preferredSurface == TranslationSessionSurface.None) return
    // Leaving fullscreen returns to the popup it was opened from; only its close button ends the translation.
    val returnToPopup = onCollapse.takeIf {
        expanded && collapsedSurface == TranslationSessionSurface.AnchoredPopup
    }

    @Composable
    fun Sheet() {
        TranslationSessionSheet(
            state = active,
            onDismiss = onDismiss,
            onCollapse = returnToPopup,
            onExecute = onExecute,
            onRetry = onRetry,
            onCopy = onCopy,
            onExpand = onExpand,
            onSelectSource = onSelectSource,
            onSelectTarget = onSelectTarget,
            onSelectEngine = onSelectEngine,
            onExternalAction = onExternalAction,
            languageSuggestions = languageSuggestions,
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
    val edgeMargin = with(density) { KatariTheme.spacing.l.roundToPx() }
    // Text selection handles reach below the selected glyphs and take touches around them; keeping the popup a
    // touch target away stops a handle from consuming taps meant for the popup's actions.
    val anchorGap = with(density) { KatariTheme.sizes.touchTarget.roundToPx() }

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
            popupAvoids,
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
                avoid = popupAvoids?.roundToIntRect(),
                onPlacementAvailabilityChanged = { availability ->
                    if (placementAvailability != availability) {
                        placementAvailability = availability
                    }
                },
                onPopupBoundsChanged = { bounds -> currentOnPopupBoundsChanged.value(bounds) },
            )
        }

        TranslationSessionPopup(
            positionProvider = positionProvider,
            state = active,
            width = with(density) { safeWidth.toDp() },
            visible = placementAvailability == TranslationPopupPlacementAvailability.Fits,
            onDismiss = onDismiss,
            onExecute = onExecute,
            onRetry = onRetry,
            onCopy = onCopy,
            onExpand = onExpand,
            onSelectSource = onSelectSource,
            onSelectTarget = onSelectTarget,
            onSelectEngine = onSelectEngine,
            onExternalAction = onExternalAction,
            languageSuggestions = languageSuggestions,
            speechState = speechState,
            onSpeechToggle = onSpeechToggle,
        )
    }
}
