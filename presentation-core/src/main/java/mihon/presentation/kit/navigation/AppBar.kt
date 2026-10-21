package mihon.presentation.kit.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.icons.materialsymbols.rounded.Close
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.presentation.kit.theme.style
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

typealias AppBarScrollBehavior = TopAppBarScrollBehavior

/**
 * A large bar shrinks to the compact one as the content scrolls up and grows back once it returns to the top; a compact
 * bar stays put. [state] lets a screen keep its scroll state elsewhere.
 */
@Composable
fun rememberAppBarScrollBehavior(
    // Material starts the limit unbounded and lets the bar set it on layout. A scaffold without a top bar, such as the
    // home screen around its tabs, would then collapse an invisible bar forever and swallow every scroll.
    state: TopAppBarState = rememberTopAppBarState(initialHeightOffsetLimit = 0f),
): AppBarScrollBehavior {
    return TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state)
}

/** The scroll behaviour of the enclosing [Scaffold]'s top bar, for content that has to cooperate with it. */
internal val LocalAppBarScrollBehavior = staticCompositionLocalOf<AppBarScrollBehavior?> { null }

/**
 * A screen's top bar. While [actionModeCounter] is above zero the bar switches to action mode: it shows the count,
 * a cancel button and [actionModeActions] on a raised container.
 */
@Composable
fun AppBar(
    title: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigateUp: (() -> Unit)? = null,
    navigationIcon: ImageVector? = null,
    actions: @Composable RowScope.() -> Unit = {},
    actionModeCounter: Int = 0,
    onCancelActionMode: () -> Unit = {},
    actionModeActions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: AppBarScrollBehavior? = null,
    overlay: Boolean = false,
    large: Boolean = true,
) {
    val isActionMode = actionModeCounter > 0
    AppBarLayout(
        modifier = modifier,
        titleContent = {
            if (isActionMode) {
                AppBarTitle(actionModeCounter.toString())
            } else {
                AppBarTitle(title, subtitle = subtitle)
            }
        },
        navigateUp = navigateUp,
        navigationIcon = navigationIcon,
        actions = {
            if (isActionMode) actionModeActions() else actions()
        },
        isActionMode = isActionMode,
        onCancelActionMode = onCancelActionMode,
        containerAlpha = null,
        scrollBehavior = scrollBehavior,
        overlay = overlay,
        large = large && title != null,
    )
}

/**
 * [containerAlpha] fades a raised container in over content drawn behind the bar, such as an entry's cover; null keeps
 * the plain surface. [overlay] lays the bar over pages on the see-through chrome container. Action
 * mode always shows the raised container fully.
 *
 * [large] opens the bar with the title on its own line in [TextRole.LargeTitle], shrinking to the compact bar as the
 * content scrolls. Overlays, action mode and bars over artwork are always compact.
 */
@Composable
fun AppBar(
    titleContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigateUp: (() -> Unit)? = null,
    navigationIcon: ImageVector? = null,
    actions: @Composable RowScope.() -> Unit = {},
    isActionMode: Boolean = false,
    onCancelActionMode: () -> Unit = {},
    containerAlpha: (() -> Float)? = null,
    scrollBehavior: AppBarScrollBehavior? = null,
    overlay: Boolean = false,
    large: Boolean = true,
) {
    AppBarLayout(
        titleContent = titleContent,
        modifier = modifier,
        navigateUp = navigateUp,
        navigationIcon = navigationIcon,
        actions = actions,
        isActionMode = isActionMode,
        onCancelActionMode = onCancelActionMode,
        containerAlpha = containerAlpha,
        scrollBehavior = scrollBehavior,
        overlay = overlay,
        large = large,
    )
}

@Composable
private fun AppBarLayout(
    titleContent: @Composable () -> Unit,
    modifier: Modifier,
    navigateUp: (() -> Unit)?,
    navigationIcon: ImageVector?,
    actions: @Composable RowScope.() -> Unit,
    isActionMode: Boolean,
    onCancelActionMode: () -> Unit,
    containerAlpha: (() -> Float)?,
    scrollBehavior: AppBarScrollBehavior?,
    overlay: Boolean,
    large: Boolean,
) {
    val colors = KatariTheme.colors
    val containerColor = when {
        isActionMode -> colors.surfaceContainer
        overlay -> colors.chrome
        containerAlpha != null -> colors.surfaceContainer.copy(alpha = containerAlpha())
        else -> colors.surface
    }
    val barColors = TopAppBarDefaults.topAppBarColors(
        containerColor = containerColor,
        scrolledContainerColor = containerColor,
    )
    val navigation = @Composable {
        if (isActionMode) {
            IconButton(
                icon = MaterialSymbols.Rounded.Close,
                contentDescription = stringResource(MR.strings.action_cancel),
                onClick = onCancelActionMode,
            )
        } else if (navigateUp != null) {
            UpButton(onClick = navigateUp, icon = navigationIcon)
        }
    }
    if (large && !isActionMode && !overlay && containerAlpha == null) {
        CollapsingAppBar(
            titleContent = titleContent,
            modifier = modifier,
            navigation = navigation,
            actions = actions,
            colors = barColors,
            scrollBehavior = scrollBehavior,
        )
    } else {
        // A one-row bar under a collapsing behaviour would scroll away whole. It stays put instead, and the shared
        // state gets no room to collapse into, so the content scrolls from the first pixel.
        scrollBehavior?.state?.let { state ->
            SideEffect {
                state.heightOffsetLimit = 0f
                state.heightOffset = 0f
            }
        }
        TopAppBar(
            modifier = modifier,
            navigationIcon = navigation,
            title = titleContent,
            actions = actions,
            colors = barColors,
        )
    }
}

/**
 * The compact bar with the title repeated large on a row under it. The row shrinks away as [scrollBehavior] collapses,
 * and the compact title fades in as it goes. The row is as tall as the title it holds, so a subtitle or a second line
 * gets its room, and the scroll state may collapse exactly that much.
 *
 * Material's TwoRowsTopAppBar does the same, but its titles crossfade from graphicsLayer blocks that never re-ran as
 * the bar collapsed here, leaving the collapsed bar without a title. This reads the collapse in composition instead.
 */
@Composable
private fun CollapsingAppBar(
    titleContent: @Composable () -> Unit,
    modifier: Modifier,
    navigation: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    colors: TopAppBarColors,
    scrollBehavior: AppBarScrollBehavior?,
) {
    val state = scrollBehavior?.state
    // Not state.collapsedFraction: the limit is a plain field, set during layout below, and collapsedFraction skips
    // reading heightOffset while the limit is still 0, so the first composition would never be invalidated.
    val heightOffset = state?.heightOffset ?: 0f
    val heightOffsetLimit = state?.heightOffsetLimit ?: 0f
    val collapsed = if (heightOffsetLimit != 0f) (heightOffset / heightOffsetLimit).coerceIn(0f, 1f) else 0f
    Column(modifier = modifier.background(colors.containerColor)) {
        TopAppBar(
            navigationIcon = navigation,
            title = { Box(Modifier.alpha(collapsed)) { titleContent() } },
            actions = actions,
            colors = colors,
        )
        Layout(
            content = {
                ProvideTextStyle(TextRole.LargeTitle.style(MaterialTheme.typography)) {
                    CompositionLocalProvider(LocalTitleMaxLines provides LARGE_TITLE_MAX_LINES) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = KatariTheme.spacing.l)
                                .padding(top = KatariTheme.spacing.xs, bottom = KatariTheme.spacing.m)
                                .alpha(1f - collapsed),
                        ) {
                            titleContent()
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds(),
        ) { measurables, constraints ->
            val title = measurables.single().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
            val expandedHeight = title.height
            if (state != null && state.heightOffsetLimit != -expandedHeight.toFloat()) {
                state.heightOffsetLimit = -expandedHeight.toFloat()
            }
            val visibleHeight = (expandedHeight + (state?.heightOffset ?: 0f)).roundToInt().coerceIn(0, expandedHeight)
            layout(constraints.maxWidth, visibleHeight) {
                // Bottom-aligned, so the title slides up under the compact row as the row shrinks.
                title.place(0, visibleHeight - expandedHeight)
            }
        }
    }
}

/**
 * How many lines an [AppBarTitle] may take. The compact bar has room for one; the large title row grows with its title,
 * so a long one wraps there instead of losing its end.
 */
private val LocalTitleMaxLines = staticCompositionLocalOf { 1 }
private const val LARGE_TITLE_MAX_LINES = 2

@Composable
fun AppBarTitle(
    title: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleSuffix: @Composable (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        title?.let {
            Row(
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = LocalTitleMaxLines.current,
                    overflow = TextOverflow.Ellipsis,
                )
                titleSuffix?.invoke()
            }
        }
        subtitle?.let {
            Text(
                text = it,
                modifier = Modifier.basicMarquee(repeatDelayMillis = 2_000),
                role = TextRole.Body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The back arrow, or [icon] where "up" means something else, such as closing a full-screen editor. */
@Composable
fun UpButton(onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    IconButton(
        icon = icon ?: MaterialSymbols.AutoMirroredRounded.ArrowBack,
        contentDescription = stringResource(MR.strings.action_bar_up_description),
        onClick = onClick,
        modifier = modifier,
    )
}
