package tachiyomi.presentation.core.components.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.padding

/** Shared reader bottom bar with feature-owned controls. */
@Composable
fun ReaderChromeBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(ReaderChromeDefaults.containerColor())
                .padding(horizontal = MaterialTheme.padding.small)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .pointerInput(Unit) {},
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** Reader chrome action whose full share of the bottom bar is touchable. */
@Composable
fun RowScope.ReaderChromeBottomBarAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .weight(1f)
            .heightIn(min = 56.dp),
        content = content,
    )
}
