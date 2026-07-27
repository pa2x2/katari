@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package mihon.entry.interactions.book

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.reader.ReaderPageIndicator
import tachiyomi.presentation.core.components.reader.ReaderProgressIndicator

/**
 * Owns the bottom safe area for BOOK reader content and progress.
 *
 * Processor chrome remains a full-screen overlay, while the reading viewport ends above the
 * measured progress footer. Native processor views can opt into the same viewport through
 * [nativeContentView] without reproducing inset or indicator sizing logic.
 */
@Composable
internal fun BookReaderScaffold(
    progress: BookReaderProgress?,
    progressVisible: Boolean,
    footerColor: Color,
    modifier: Modifier = Modifier,
    nativeContentView: View? = null,
    content: @Composable BoxScope.() -> Unit,
    overlay: @Composable BoxScope.() -> Unit = {},
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

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                content = content,
            )
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
                progress?.let {
                    BookReaderProgressIndicator(
                        progress = it,
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .graphicsLayer { alpha = if (progressVisible) 1f else 0f }
                            .semantics {
                                if (!progressVisible) hideFromAccessibility()
                            },
                    )
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            content = overlay,
        )
    }
}

internal sealed interface BookReaderProgress {
    data class Page(
        val currentPage: Int,
        val totalPages: Int,
    ) : BookReaderProgress

    data class Percentage(val value: Int) : BookReaderProgress
}

@Composable
private fun BookReaderProgressIndicator(
    progress: BookReaderProgress,
    modifier: Modifier,
) {
    when (progress) {
        is BookReaderProgress.Page -> ReaderPageIndicator(
            currentPage = progress.currentPage,
            totalPages = progress.totalPages,
            modifier = modifier,
        )
        is BookReaderProgress.Percentage -> ReaderProgressIndicator(
            text = "${progress.value}%",
            modifier = modifier,
        )
    }
}

private fun View.setBottomMargin(bottomMargin: Int) {
    val margins = layoutParams as? ViewGroup.MarginLayoutParams ?: return
    if (margins.bottomMargin == bottomMargin) return
    margins.bottomMargin = bottomMargin
    layoutParams = margins
}
