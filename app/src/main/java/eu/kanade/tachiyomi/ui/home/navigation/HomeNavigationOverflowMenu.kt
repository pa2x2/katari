package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

@Composable
internal fun HomeNavigationOverflowMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return

    val density = LocalDensity.current
    val positionProvider = remember(density) {
        HomeNavigationOverflowPositionProvider(
            gap = with(density) { NavigationAnchorGap.roundToPx() },
            windowMargin = with(density) { WindowMargin.roundToPx() },
        )
    }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            modifier = Modifier.sizeIn(minWidth = MenuWidth, maxWidth = MenuWidth),
            shape = HomeNavigationOverflowShape,
            color = MenuDefaults.containerColor,
            tonalElevation = MenuDefaults.TonalElevation,
            shadowElevation = MenuDefaults.ShadowElevation,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                content = content,
            )
        }
    }
}

internal class HomeNavigationOverflowPositionProvider(
    private val gap: Int,
    private val windowMargin: Int,
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val horizontalMargin = windowMargin.coerceAtMost(
            (windowSize.width - popupContentSize.width).coerceAtLeast(0) / 2,
        )
        val minX = horizontalMargin
        val maxX = (windowSize.width - horizontalMargin - popupContentSize.width).coerceAtLeast(minX)
        val anchorAlignedX = when (layoutDirection) {
            LayoutDirection.Ltr -> anchorBounds.right - popupContentSize.width
            LayoutDirection.Rtl -> anchorBounds.left
        }
        val x = anchorAlignedX.coerceIn(minX, maxX)

        val verticalMargin = windowMargin.coerceAtMost(
            (windowSize.height - popupContentSize.height).coerceAtLeast(0) / 2,
        )
        val aboveAnchor = anchorBounds.top - gap - popupContentSize.height
        val belowAnchor = anchorBounds.bottom + gap
        val maxY = (windowSize.height - verticalMargin - popupContentSize.height).coerceAtLeast(verticalMargin)
        val y = if (aboveAnchor >= verticalMargin) aboveAnchor else belowAnchor.coerceIn(verticalMargin, maxY)

        return IntOffset(x, y)
    }
}

private val MenuWidth = 196.dp
internal val HomeNavigationOverflowShape = RoundedCornerShape(12.dp)
private val NavigationAnchorGap = 8.dp
private val WindowMargin = 8.dp
