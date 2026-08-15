@file:OptIn(ExperimentalLayoutApi::class)

package mihon.entry.interactions.book.reader

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import mihon.entry.interactions.book.reader.layout.BookReaderRoundedCornerSafeArea
import mihon.entry.interactions.book.reader.translation.BookSelectionTranslationController
import mihon.translation.ui.presentation.CoordinatedTranslationSessionHost
import mihon.translation.ui.presentation.TranslationResultSpeechState
import mihon.translation.ui.presentation.TranslationResultSpeechTarget

/**
 * Owns the safe reading viewport and progress placement for BOOK reader content.
 *
 * Processor chrome remains a full-screen overlay. Ambient progress is drawn inside the reading
 * viewport, while footer progress reserves space below it and moves with chrome. Native processor
 * views can opt into the same viewport through [nativeContentView] without reproducing inset or
 * progress sizing logic.
 */
@Composable
internal fun BookReaderScaffold(
    progress: BookReaderProgress?,
    footerColor: Color,
    modifier: Modifier = Modifier,
    nativeContentView: View? = null,
    translationController: BookSelectionTranslationController? = null,
    translationSpeechState: TranslationResultSpeechState = TranslationResultSpeechState(),
    onTranslationSpeechToggle: ((TranslationResultSpeechTarget) -> Unit)? = null,
    onTranslationPopupBoundsChanged: (Rect?) -> Unit = {},
    translationTheme: @Composable (@Composable () -> Unit) -> Unit = { content -> content() },
    onRootPositionInWindow: (Offset) -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
    overlay: @Composable BoxScope.(@Composable () -> Unit) -> Unit = {},
) {
    var footerHeight by remember { mutableIntStateOf(0) }
    val originalNativeBottomMargin = remember(nativeContentView) {
        (nativeContentView?.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin
    }

    DisposableEffect(nativeContentView, originalNativeBottomMargin) {
        onDispose {
            if (originalNativeBottomMargin != null) {
                nativeContentView?.setBottomMargin(originalNativeBottomMargin)
            }
        }
    }
    SideEffect {
        if (originalNativeBottomMargin != null) {
            nativeContentView?.setBottomMargin(originalNativeBottomMargin + footerHeight)
        }
    }

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
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(
                                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                                ),
                            ),
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
                        WindowInsets.navigationBarsIgnoringVisibility.only(WindowInsetsSides.Bottom),
                    )
                    .onSizeChanged { footerHeight = it.height },
                contentAlignment = Alignment.Center,
            ) {
                progress?.takeIf { it.usesFooter }?.let {
                    BookReaderFooterProgressIndicator(
                        progress = it,
                        modifier = Modifier
                            .padding(vertical = 2.dp)
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
                progress?.takeIf { it.usesFooter }?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(
                                WindowInsets.navigationBarsIgnoringVisibility.only(WindowInsetsSides.Bottom),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        BookReaderFooterProgressIndicator(
                            progress = it,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }
        translationController?.let { controller ->
            translationTheme {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    CoordinatedTranslationSessionHost(
                        coordinator = controller.hostCoordinator,
                        isTabletUi = maxWidth >= 720.dp,
                        modifier = Modifier.fillMaxSize(),
                        onDismiss = controller::dismissTranslation,
                        onPopupBoundsChanged = onTranslationPopupBoundsChanged,
                        speechState = translationSpeechState,
                        onSpeechToggle = onTranslationSpeechToggle,
                    )
                }
            }
        }
    }
}

private fun View.setBottomMargin(bottomMargin: Int) {
    val margins = layoutParams as? ViewGroup.MarginLayoutParams ?: return
    if (margins.bottomMargin == bottomMargin) return
    margins.bottomMargin = bottomMargin
    layoutParams = margins
}
