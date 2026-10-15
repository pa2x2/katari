package eu.kanade.presentation.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import eu.kanade.tachiyomi.data.library.LibraryUpdateProgress
import eu.kanade.tachiyomi.data.library.LibraryUpdateScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
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
    val scopeLabel = progress.scopeName?.let { stringResource(MR.strings.updating_named, it) } ?: stringResource(
        when (progress.scope) {
            LibraryUpdateScope.Library -> MR.strings.updating_library
            LibraryUpdateScope.Category -> MR.strings.updating_category
            LibraryUpdateScope.Source -> MR.strings.updating_extension
            LibraryUpdateScope.Type -> MR.strings.updating_type
            LibraryUpdateScope.Group -> MR.strings.updating_group
            LibraryUpdateScope.Selection -> MR.strings.updating_selection
        },
    )
    val skippedLabel = progress.skipped.takeIf { it > 0 }?.let {
        pluralStringResource(MR.plurals.library_update_skipped_by_rules, it, it)
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        container = SurfaceContainer.Default,
    ) {
        Row(
            modifier = Modifier.padding(start = KatariTheme.spacing.l, end = KatariTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            ) {
                Text(
                    text = listOfNotNull(
                        if (progress.total > 0) {
                            stringResource(
                                MR.strings.library_update_progress,
                                scopeLabel,
                                progress.completed,
                                progress.total,
                            )
                        } else {
                            scopeLabel
                        },
                        skippedLabel,
                    )
                        .joinToString(" · "),
                    role = TextRole.Supporting,
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
            IconButton(
                icon = MaterialSymbols.Rounded.Close,
                contentDescription = stringResource(MR.strings.library_update_cancel),
                onClick = onCancel,
            )
        }
    }
}
