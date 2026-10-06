package eu.kanade.presentation.more.stats.recap.story

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.recap.palette.RecapPalette
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * One bar per page: pages already seen are full, the current one fills as it plays.
 *
 * @param progress how far the current page has played, read on every frame while drawing only.
 */
@Composable
internal fun RecapProgressBars(
    count: Int,
    index: Int,
    progress: () -> Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = modifier.fillMaxWidth().height(3.dp)) {
        repeat(count) { page ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(color.copy(alpha = 0.3f))
                    .drawBehind {
                        val filled = when {
                            page < index -> 1f
                            page == index -> progress().coerceIn(0f, 1f)
                            else -> 0f
                        }
                        drawRect(color, size = size.copy(width = size.width * filled))
                    },
            )
        }
    }
}

@Composable
internal fun RecapTopBar(
    title: String,
    palette: RecapPalette,
    onMore: () -> Unit,
    onClose: () -> Unit,
    moreMenu: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = palette.muted,
            modifier = Modifier.weight(1f).padding(start = 16.dp),
        )
        Box {
            IconButton(onClick = onMore) {
                Icon(Icons.Outlined.MoreVert, stringResource(MR.strings.action_menu), tint = palette.ink)
            }
            moreMenu()
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Outlined.Close, stringResource(MR.strings.action_close), tint = palette.ink)
        }
    }
}

/** Share for any page; on the last page also a way to watch the story again. */
@Composable
internal fun RecapBottomBar(
    isLastPage: Boolean,
    canReplay: Boolean,
    palette: RecapPalette,
    onShare: () -> Unit,
    onReplay: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(
            12.dp,
            if (isLastPage) Alignment.CenterHorizontally else Alignment.End,
        ),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
    ) {
        if (isLastPage) {
            Button(
                onClick = onShare,
                colors = ButtonDefaults.buttonColors(containerColor = palette.ink, contentColor = palette.background),
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(MR.strings.action_share))
            }
            if (canReplay) {
                OutlinedButton(
                    onClick = onReplay,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.ink),
                ) {
                    Text(stringResource(MR.strings.statistics_recap_watch_again))
                }
            }
        } else {
            IconButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, stringResource(MR.strings.action_share), tint = palette.ink)
            }
        }
    }
}
