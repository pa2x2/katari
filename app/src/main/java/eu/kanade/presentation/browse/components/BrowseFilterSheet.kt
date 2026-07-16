package eu.kanade.presentation.browse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun BrowseFilterSheet(
    title: String,
    onDismissRequest: () -> Unit,
    onReset: (() -> Unit)? = null,
    resetEnabled: Boolean = true,
    content: @Composable (Modifier) -> Unit,
) {
    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
                    .align(Alignment.CenterHorizontally),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MaterialTheme.padding.medium,
                        end = MaterialTheme.padding.small,
                        top = MaterialTheme.padding.medium,
                        bottom = MaterialTheme.padding.small,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                onReset?.let {
                    TextButton(
                        onClick = it,
                        enabled = resetEnabled,
                    ) {
                        Text(stringResource(MR.strings.action_reset))
                    }
                }
            }

            content(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.padding.medium),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(onClick = onDismissRequest) {
                    Text(stringResource(MR.strings.action_done))
                }
            }
        }
    }
}
