package mihon.presentation.kit.navigation

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.icons.materialsymbols.rounded.Close
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

typealias AppBarScrollBehavior = TopAppBarScrollBehavior

/** The bar stays put while the content scrolls under it; [state] lets a screen keep its scroll state elsewhere. */
@Composable
fun rememberPinnedAppBarScrollBehavior(state: TopAppBarState = rememberTopAppBarState()): AppBarScrollBehavior {
    return TopAppBarDefaults.pinnedScrollBehavior(state)
}

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
) {
    val isActionMode = actionModeCounter > 0
    AppBar(
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
        scrollBehavior = scrollBehavior,
        overlay = overlay,
    )
}

/**
 * [containerAlpha] fades a raised container in over content drawn behind the bar, such as an entry's cover; null keeps
 * the plain surface. [overlay] lays the bar over pages on the see-through chrome container. Action
 * mode always shows the raised container fully.
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
) {
    val colors = KatariTheme.colors
    val containerColor = when {
        isActionMode -> colors.surfaceContainer
        overlay -> colors.chrome
        containerAlpha != null -> colors.surfaceContainer.copy(alpha = containerAlpha())
        else -> colors.surface
    }
    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            if (isActionMode) {
                IconButton(
                    icon = MaterialSymbols.Rounded.Close,
                    contentDescription = stringResource(MR.strings.action_cancel),
                    onClick = onCancelActionMode,
                )
            } else {
                navigateUp?.let { UpButton(onClick = it, icon = navigationIcon) }
            }
        },
        title = titleContent,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
        ),
        scrollBehavior = scrollBehavior,
    )
}

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
                Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
