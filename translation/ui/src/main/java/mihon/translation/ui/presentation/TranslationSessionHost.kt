package mihon.translation.ui.presentation

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mihon.translation.ui.session.TranslationSessionController
import mihon.translation.ui.session.TranslationSessionState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.AdaptiveSheet
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.min

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
) {
    val state by controller.state.collectAsState()
    val active = state as? TranslationSessionState.Active
    var expanded by remember(active?.input?.request) { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val clipboardLabel = stringResource(MR.strings.translation_title)

    DisposableEffect(controller) {
        onDispose(controller::dismiss)
    }

    TranslationSessionOverlay(
        state = state,
        expanded = expanded,
        isTabletUi = isTabletUi,
        onDismiss = controller::dismiss,
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
    onSelectSource: (mihon.translation.api.TranslationLanguageTag) -> Unit,
    onSelectEngine: (mihon.translation.api.TranslationEngineSelection) -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = state as? TranslationSessionState.Active ?: return
    val preferredSurface = if (expanded) {
        TranslationSessionSurface.AdaptiveSheet
    } else {
        state.preferredSurface()
    }
    if (preferredSurface == TranslationSessionSurface.None) return

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val leftInset = WindowInsets.safeDrawing.getLeft(density, layoutDirection)
    val topInset = WindowInsets.safeDrawing.getTop(density)
    val rightInset = WindowInsets.safeDrawing.getRight(density, layoutDirection)
    val bottomInset = WindowInsets.safeDrawing.getBottom(density)
    val edgeMargin = with(density) { POPUP_EDGE_MARGIN.roundToPx() }
    val anchorGap = with(density) { POPUP_ANCHOR_GAP.roundToPx() }
    val popupMaximumWidth = with(density) { POPUP_MAXIMUM_WIDTH.roundToPx() }

    SubcomposeLayout(modifier = modifier.fillMaxSize()) { constraints ->
        fun sheet() = subcompose(TranslationSessionSlot.Sheet) {
            AdaptiveSheet(
                isTabletUi = isTabletUi,
                enableImplicitDismiss = true,
                onDismissRequest = onDismiss,
                modifier = Modifier.testTag(TRANSLATION_SESSION_SHEET_TAG),
            ) {
                TranslationSessionContent(
                    state = active,
                    expanded = true,
                    onDismiss = onDismiss,
                    onExecute = onExecute,
                    onRetry = onRetry,
                    onCopy = onCopy,
                    onExpand = onExpand,
                    onSelectSource = onSelectSource,
                    onSelectEngine = onSelectEngine,
                    onExternalAction = onExternalAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                )
            }
        }.single().measure(constraints)

        if (preferredSurface == TranslationSessionSurface.AdaptiveSheet) {
            val sheet = sheet()
            return@SubcomposeLayout layout(constraints.maxWidth, constraints.maxHeight) {
                sheet.place(0, 0)
            }
        }

        val safeWidth = constraints.maxWidth - leftInset - rightInset - edgeMargin * 2
        val safeHeight = constraints.maxHeight - topInset - bottomInset - edgeMargin * 2
        if (safeWidth <= 0 || safeHeight <= 0) {
            val sheet = sheet()
            return@SubcomposeLayout layout(constraints.maxWidth, constraints.maxHeight) {
                sheet.place(0, 0)
            }
        }

        val popup = subcompose(TranslationSessionSlot.Popup) {
            TranslationSessionPopup(
                state = active,
                onDismiss = onDismiss,
                onExecute = onExecute,
                onRetry = onRetry,
                onCopy = onCopy,
                onExpand = onExpand,
                onSelectSource = onSelectSource,
                onSelectEngine = onSelectEngine,
                onExternalAction = onExternalAction,
            )
        }.single().measure(
            Constraints(
                maxWidth = min(popupMaximumWidth, safeWidth),
                maxHeight = Constraints.Infinity,
            ),
        )
        val anchor = active.input.anchor
        val placement = anchor?.let {
            calculateTranslationPopupPlacement(
                anchor = it,
                popup = TranslationPopupSize(popup.width, popup.height),
                viewport = TranslationViewportBounds(
                    left = leftInset,
                    top = topInset,
                    right = constraints.maxWidth - rightInset,
                    bottom = constraints.maxHeight - bottomInset,
                ),
                edgeMargin = edgeMargin,
                anchorGap = anchorGap,
            )
        }
        if (placement == null) {
            val sheet = sheet()
            layout(constraints.maxWidth, constraints.maxHeight) {
                sheet.place(0, 0)
            }
        } else {
            layout(constraints.maxWidth, constraints.maxHeight) {
                popup.place(placement.x, placement.y)
            }
        }
    }
}

@Composable
private fun TranslationSessionPopup(
    state: TranslationSessionState.Active,
    onDismiss: () -> Unit,
    onExecute: () -> Unit,
    onRetry: () -> Unit,
    onCopy: (String) -> Unit,
    onExpand: () -> Unit,
    onSelectSource: (mihon.translation.api.TranslationLanguageTag) -> Unit,
    onSelectEngine: (mihon.translation.api.TranslationEngineSelection) -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Box {
        Surface(
            modifier = Modifier.testTag(TRANSLATION_SESSION_POPUP_TAG),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            TranslationSessionContent(
                state = state,
                expanded = false,
                onDismiss = onDismiss,
                onExecute = onExecute,
                onRetry = onRetry,
                onCopy = onCopy,
                onExpand = onExpand,
                onSelectSource = onSelectSource,
                onSelectEngine = onSelectEngine,
                onExternalAction = onExternalAction,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private enum class TranslationSessionSlot {
    Popup,
    Sheet,
}

private val POPUP_EDGE_MARGIN = 16.dp
private val POPUP_ANCHOR_GAP = 8.dp
private val POPUP_MAXIMUM_WIDTH = 360.dp

internal const val TRANSLATION_SESSION_POPUP_TAG = "translation_session_popup"
internal const val TRANSLATION_SESSION_SHEET_TAG = "translation_session_sheet"
