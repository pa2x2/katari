@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package tachiyomi.presentation.core.components.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset

private val readerBarsSlideAnimationSpec = tween<IntOffset>(200)
private val readerPersistentContentSlideAnimationSpec = tween<Int>(200)
private val readerBarsFadeAnimationSpec = tween<Float>(150)

/** Animated reader chrome shared by processor-owned reader implementations. */
@Composable
fun ReaderChrome(
    visible: Boolean,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    middleContent: @Composable BoxScope.() -> Unit = {},
    persistentBottomContent: @Composable () -> Unit = {},
) {
    var bottomBarHeight by remember { mutableIntStateOf(0) }
    val navigationBarHeight = WindowInsets.navigationBarsIgnoringVisibility.getBottom(LocalDensity.current)
    val persistentBottomContentOffset by animateIntAsState(
        targetValue = if (visible) {
            (bottomBarHeight - navigationBarHeight).coerceAtLeast(0)
        } else {
            0
        },
        animationSpec = readerPersistentContentSlideAnimationSpec,
        label = "readerPersistentBottomContentOffset",
    )

    Box(modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.fillMaxHeight()) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(readerBarsSlideAnimationSpec) { -it } + fadeIn(readerBarsFadeAnimationSpec),
                exit = slideOutVertically(readerBarsSlideAnimationSpec) { -it } + fadeOut(readerBarsFadeAnimationSpec),
                content = { topBar() },
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                content = middleContent,
            )

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(readerBarsSlideAnimationSpec) { it } + fadeIn(readerBarsFadeAnimationSpec),
                exit = slideOutVertically(readerBarsSlideAnimationSpec) { it } + fadeOut(readerBarsFadeAnimationSpec),
                content = {
                    Box(modifier = Modifier.onSizeChanged { bottomBarHeight = it.height }) {
                        bottomBar()
                    }
                },
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer { translationY = -persistentBottomContentOffset.toFloat() },
            contentAlignment = Alignment.Center,
        ) {
            persistentBottomContent()
        }
    }
}
