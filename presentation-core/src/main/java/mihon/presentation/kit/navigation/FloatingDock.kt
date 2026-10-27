package mihon.presentation.kit.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.theme.KatariSizes
import mihon.presentation.kit.theme.KatariSpacing
import mihon.presentation.kit.theme.KatariTheme

/**
 * Where a [FloatingDock] floats: across the bottom edge of a narrow window, or down the start edge of a wide one, where
 * it takes no height and stays under the thumb. The same pill and items serve both, so folding a foldable only moves
 * it.
 */
enum class DockPlacement { Bottom, Side }

/**
 * Lays [dock], typically a [FloatingDock], over [content] at the [placement] edge. Content scrolls on under the dock:
 * every [Scaffold] inside adds the dock's footprint to its bottom or start padding and keeps its FAB and snackbars clear
 * of it, for as long as [dockVisible] holds.
 */
@Composable
fun FloatingDockHost(
    dockVisible: Boolean,
    dock: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    placement: DockPlacement = DockPlacement.Bottom,
    content: @Composable () -> Unit,
) {
    // Docks nest, a bottom one in the home tabs inside the side one of the whole app, so each adds to what's outside it
    val outer = LocalDockClearance.current
    val clearance = when {
        !dockVisible -> outer
        placement == DockPlacement.Bottom -> outer.copy(bottom = BottomDockClearance)
        else -> outer.copy(start = SideDockClearance)
    }
    Box(modifier = modifier) {
        CompositionLocalProvider(LocalDockClearance provides clearance, content = content)
        when (placement) {
            DockPlacement.Bottom -> AnimatedVisibility(
                visible = dockVisible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) { dock() }
            DockPlacement.Side -> AnimatedVisibility(
                visible = dockVisible,
                modifier = Modifier.align(Alignment.CenterStart),
                enter = slideInHorizontally { -it } + fadeIn(),
                exit = slideOutHorizontally { -it } + fadeOut(),
            ) { dock() }
        }
    }
}

/**
 * The home destinations on a pill floating at the [placement] edge, centred and only as long as its items. Fill it with
 * [FloatingDockItem]s.
 */
@Composable
fun FloatingDock(
    modifier: Modifier = Modifier,
    placement: DockPlacement = DockPlacement.Bottom,
    content: @Composable DockScope.() -> Unit,
) {
    val edgeModifier = when (placement) {
        DockPlacement.Bottom ->
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                )
                .padding(horizontal = KatariTheme.spacing.l)
                .padding(bottom = DockMargin)
        DockPlacement.Side ->
            Modifier
                .fillMaxHeight()
                .windowInsetsPadding(
                    WindowInsets.navigationBars.union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Start + WindowInsetsSides.Vertical),
                )
                .padding(vertical = KatariTheme.spacing.l)
                .padding(start = DockMargin)
    }
    Box(modifier = modifier.then(edgeModifier), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Surface(
            modifier = Modifier.testTag(FLOATING_DOCK_TEST_TAG),
            shape = KatariTheme.shapes.full,
            color = KatariTheme.colors.surfaceContainerHigh,
            contentColor = KatariTheme.colors.onSurface,
            shadowElevation = DockElevation,
        ) {
            when (placement) {
                DockPlacement.Bottom -> Row(
                    modifier = Modifier
                        .padding(DockPadding)
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) { BottomDockScope(this).content() }
                DockPlacement.Side -> Column(
                    modifier = Modifier
                        .padding(DockPadding)
                        .selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { SideDockScope(this).content() }
            }
        }
    }
}

/** How far a [Scaffold] keeps its content, FAB and snackbars from the edges a [FloatingDock] floats over. */
@Immutable
internal data class DockClearance(val bottom: Dp = 0.dp, val start: Dp = 0.dp) {
    companion object {
        val None = DockClearance()
    }
}

internal val LocalDockClearance = compositionLocalOf { DockClearance.None }

// UI automation, such as the showcase capture scenes, finds the pill by this to keep what it taps out from under it.
private const val FLOATING_DOCK_TEST_TAG = "floating_dock"

internal val DockPadding = KatariSpacing.s
private val DockMargin = KatariSpacing.m
private val DockElevation = 6.dp

// The dock's footprint from its edge, plus a gap so content doesn't end flush against it.
private val BottomDockClearance: Dp = DockMargin + DockPadding * 2 + KatariSizes.touchTarget + KatariSpacing.s
private val SideDockClearance: Dp = DockMargin + DockPadding * 2 + SideDockItemWidth + KatariSpacing.s
