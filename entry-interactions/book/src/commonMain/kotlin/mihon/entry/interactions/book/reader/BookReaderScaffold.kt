package mihon.entry.interactions.book.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import mihon.core.platform.clipboard.TextClipboard
import mihon.core.platform.power.PowerSource
import mihon.entry.interactions.book.reader.layout.BookReaderRoundedCornerSafeArea
import mihon.entry.interactions.book.reader.layout.bookReaderPageInsets
import mihon.entry.interactions.book.reader.translation.BookSelectionTranslationController
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.window.stableNavigationBars
import mihon.translation.ui.presentation.CoordinatedTranslationSessionHost
import mihon.translation.ui.presentation.TranslationResultSpeechState
import mihon.translation.ui.presentation.TranslationResultSpeechTarget

/**
 * Owns the safe reading viewport and progress placement for BOOK reader content.
 *
 * Processor chrome remains a full-screen overlay. Ambient progress is drawn inside the reading
 * viewport, while footer progress reserves space below it and moves with chrome.
 */
@Composable
internal fun BookReaderScaffold(
    progress: BookReaderProgress?,
    footerColor: Color,
    modifier: Modifier = Modifier,
    showTimeAndBattery: Boolean,
    powerSource: PowerSource,
    translationController: BookSelectionTranslationController? = null,
    /** Takes what the translation popup copies. */
    textClipboard: TextClipboard,
    /** Opens the documentation links the translation popup offers. */
    openLink: (String) -> Unit,
    translationSpeechState: TranslationResultSpeechState = TranslationResultSpeechState(),
    onTranslationSpeechToggle: ((TranslationResultSpeechTarget) -> Unit)? = null,
    onTranslationPopupBoundsChanged: (Rect?) -> Unit = {},
    translationSnackbarHostState: SnackbarHostState? = null,
    translationTheme: @Composable (@Composable () -> Unit) -> Unit = { content -> content() },
    onRootPositionInWindow: (Offset) -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
    overlay: @Composable BoxScope.(@Composable () -> Unit) -> Unit = {},
) {
    val deviceStatus = if (showTimeAndBattery) {
        val time by rememberDeviceTime()
        val battery by remember(powerSource) { powerSource.batteryPercent() }.collectAsState(null)
        BookReaderDeviceStatus(time, battery)
    } else {
        null
    }
    val showsFooter = progress.showsFooter(deviceStatus)

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            onRootPositionInWindow(coordinates.positionInWindow())
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                content()
                progress?.let {
                    BookReaderRoundedCornerSafeArea(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(bookReaderPageInsets),
                    ) { roundedCornerInsets ->
                        BookReaderAmbientProgressIndicator(
                            progress = it,
                            roundedCornerInsets = roundedCornerInsets,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(footerColor)
                    .windowInsetsPadding(
                        WindowInsets.stableNavigationBars.only(WindowInsetsSides.Bottom),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (showsFooter) {
                    BookReaderFooter(
                        progress = progress,
                        deviceStatus = deviceStatus,
                        modifier = Modifier
                            .padding(vertical = KatariTheme.spacing.xxs)
                            .graphicsLayer { alpha = 0f }
                            .semantics {
                                hideFromAccessibility()
                            },
                    )
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            overlay {
                if (showsFooter) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(
                                WindowInsets.stableNavigationBars.only(WindowInsetsSides.Bottom),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        BookReaderFooter(
                            progress = progress,
                            deviceStatus = deviceStatus,
                            modifier = Modifier.padding(vertical = KatariTheme.spacing.xxs),
                        )
                    }
                }
            }
        }
        if (translationController != null) {
            translationTheme {
                CoordinatedTranslationSessionHost(
                    coordinator = translationController.hostCoordinator,
                    openLink = openLink,
                    clipboard = textClipboard,
                    modifier = Modifier.fillMaxSize(),
                    onDismiss = translationController::dismissTranslation,
                    onPopupBoundsChanged = onTranslationPopupBoundsChanged,
                    speechState = translationSpeechState,
                    onSpeechToggle = onTranslationSpeechToggle,
                    snackbarHostState = translationSnackbarHostState,
                )
            }
        }
    }
}
