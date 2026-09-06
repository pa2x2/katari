package tachiyomi.presentation.core.components.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.R

/** Shared action for returning to the position before an explicit reader jump. */
@Composable
fun ReaderReturnPositionBar(
    onReturn: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onReturn, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.reader_navigation_return))
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Outlined.Close, stringResource(R.string.reader_navigation_dismiss_return))
        }
    }
}
