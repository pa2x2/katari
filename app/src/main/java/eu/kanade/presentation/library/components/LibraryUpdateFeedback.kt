package eu.kanade.presentation.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.library.LibraryUpdateProgress
import eu.kanade.tachiyomi.data.library.LibraryUpdateScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.seconds

/**
 * Pull-to-refresh state for starting a library update: set it once the update is started, and it clears when the
 * update's progress appears, handing over to [LibraryUpdateStrip], or when the update never shows up.
 */
@Composable
internal fun rememberAwaitingUpdateState(updateProgress: LibraryUpdateProgress?): MutableState<Boolean> {
    val awaitingUpdate = remember { mutableStateOf(false) }
    val currentUpdateProgress by rememberUpdatedState(updateProgress)
    LaunchedEffect(awaitingUpdate.value) {
        if (!awaitingUpdate.value) return@LaunchedEffect
        withTimeoutOrNull(UpdateStartTimeout) {
            snapshotFlow { currentUpdateProgress }.first { it != null }
        }
        awaitingUpdate.value = false
    }
    return awaitingUpdate
}

private val UpdateStartTimeout = 10.seconds

/** Shows a running library update while there is one, keeping its last progress while it animates out. */
@Composable
internal fun LibraryUpdateStrip(
    progress: LibraryUpdateProgress?,
    onCancel: () -> Unit,
) {
    var lastProgress by remember { mutableStateOf(progress) }
    LaunchedEffect(progress) {
        if (progress != null) lastProgress = progress
    }
    AnimatedVisibility(visible = progress != null) {
        (progress ?: lastProgress)?.let {
            LibraryUpdateStripContent(progress = it, onCancel = onCancel)
        }
    }
}

/** A running library update: what it covers, how far it got, and a way to stop it. */
@Composable
private fun LibraryUpdateStripContent(
    progress: LibraryUpdateProgress,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scopeLabel = stringResource(
        when (progress.scope) {
            LibraryUpdateScope.Library -> MR.strings.updating_library
            LibraryUpdateScope.Category -> MR.strings.updating_category
            LibraryUpdateScope.Source -> MR.strings.updating_extension
            LibraryUpdateScope.Type -> MR.strings.updating_type
            LibraryUpdateScope.Group -> MR.strings.updating_group
            LibraryUpdateScope.Selection -> MR.strings.updating_selection
        },
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = if (progress.total > 0) {
                        stringResource(
                            MR.strings.library_update_progress,
                            scopeLabel,
                            progress.completed,
                            progress.total,
                        )
                    } else {
                        scopeLabel
                    },
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (progress.total > 0) {
                    LinearProgressIndicator(
                        progress = { progress.completed.toFloat() / progress.total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(MR.strings.library_update_cancel),
                )
            }
        }
    }
}
