package mihon.feature.appupdate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.components.MarkdownRender
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import mihon.feature.appupdate.AppUpdateStatus
import mihon.feature.appupdate.check.AvailableUpdate
import mihon.feature.appupdate.check.UpdateApk
import mihon.feature.appupdate.check.VersionNotes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The update prompt: what the new version brings, then download and install progress, over whatever screen is open.
 * Swiping it away is "Not now".
 */
@Composable
internal fun AppUpdateSheet(
    update: AvailableUpdate,
    status: AppUpdateStatus,
    onUpdate: () -> Unit,
    onCancelDownload: () -> Unit,
    onAllowInstalls: () -> Unit,
    onSkipVersion: () -> Unit,
    onOpenReleasePage: () -> Unit,
    onDismiss: () -> Unit,
) {
    AdaptiveSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(
                start = MaterialTheme.padding.large,
                end = MaterialTheme.padding.small,
                top = MaterialTheme.padding.large,
                bottom = MaterialTheme.padding.medium,
            ),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(MR.strings.app_update_available),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = update.summary(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onOpenReleasePage) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = stringResource(MR.strings.update_check_open),
                    )
                }
            }

            Column(modifier = Modifier.padding(end = MaterialTheme.padding.small)) {
                if (update.notes.isNotEmpty()) {
                    ReleaseNotesCard(
                        notes = update.notes,
                        modifier = Modifier.padding(top = MaterialTheme.padding.medium),
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.padding.medium))
                StatusDetail(status)
                Actions(
                    status = status,
                    onUpdate = onUpdate,
                    onCancelDownload = onCancelDownload,
                    onAllowInstalls = onAllowInstalls,
                    onSkipVersion = onSkipVersion,
                    onOpenReleasePage = onOpenReleasePage,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

/** Each version's notes under its own heading, newest first, scrolling inside the sheet. */
@Composable
private fun ReleaseNotesCard(notes: List<VersionNotes>, modifier: Modifier = Modifier) {
    val flavour = remember { GFMFlavourDescriptor() }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.padding.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            notes.forEachIndexed { index, versionNotes ->
                // One version's notes speak for themselves; several need telling apart. The label matches the notes'
                // own ### headings in size, so its colour and the divider set it above them.
                if (notes.size > 1) {
                    if (index > 0) HorizontalDivider()
                    Text(
                        text = versionNotes.version,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                MarkdownRender(
                    content = versionNotes.notes,
                    flavour = flavour,
                    listBlockSpacing = MaterialTheme.padding.extraSmall,
                    listItemSpacing = MaterialTheme.padding.extraSmall,
                    listSectionSpacing = MaterialTheme.padding.small,
                )
            }
        }
    }
}

@Composable
private fun StatusDetail(status: AppUpdateStatus) {
    val text = when (status) {
        is AppUpdateStatus.Downloading -> {
            if (status.progress != null) {
                LinearProgressIndicator(progress = { status.progress }, modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            downloadingText(status.progress)
        }
        AppUpdateStatus.NeedsPermission -> stringResource(MR.strings.app_update_permission_note)
        AppUpdateStatus.Installing -> stringResource(MR.strings.app_update_installing_note)
        is AppUpdateStatus.Failed -> status.error.message()
        else -> null
    } ?: return
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (status is AppUpdateStatus.Failed) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.padding(top = MaterialTheme.padding.small, bottom = MaterialTheme.padding.medium),
    )
}

@Composable
private fun Actions(
    status: AppUpdateStatus,
    onUpdate: () -> Unit,
    onCancelDownload: () -> Unit,
    onAllowInstalls: () -> Unit,
    onSkipVersion: () -> Unit,
    onOpenReleasePage: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        val fullWidth = Modifier.fillMaxWidth()
        when (status) {
            is AppUpdateStatus.Downloading -> FilledTonalButton(onClick = onCancelDownload, modifier = fullWidth) {
                Text(text = stringResource(MR.strings.app_update_cancel_download))
            }
            AppUpdateStatus.Installing -> Button(onClick = {}, enabled = false, modifier = fullWidth) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(MaterialTheme.padding.small))
                Text(text = stringResource(MR.strings.app_update_installing))
            }
            AppUpdateStatus.NeedsPermission -> Button(onClick = onAllowInstalls, modifier = fullWidth) {
                Text(text = stringResource(MR.strings.app_update_allow_installs))
            }
            is AppUpdateStatus.Failed -> Button(onClick = onUpdate, modifier = fullWidth) {
                Text(text = stringResource(MR.strings.action_retry))
            }
            else -> Button(onClick = onUpdate, modifier = fullWidth) {
                Text(text = stringResource(MR.strings.app_update_now))
            }
        }
        if (status is AppUpdateStatus.Failed) {
            TextButton(onClick = onOpenReleasePage, modifier = fullWidth) {
                Text(text = stringResource(MR.strings.update_check_open))
            }
        }
        if (status == AppUpdateStatus.Available) {
            TextButton(onClick = onSkipVersion, modifier = fullWidth) {
                Text(text = stringResource(MR.strings.app_update_skip_version))
            }
        }
        if (status == AppUpdateStatus.Available ||
            status == AppUpdateStatus.NeedsPermission ||
            status is AppUpdateStatus.Failed
        ) {
            TextButton(onClick = onDismiss, modifier = fullWidth) {
                Text(text = stringResource(MR.strings.action_not_now))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun AppUpdateSheetPreview() {
    TachiyomiPreviewTheme {
        AppUpdateSheet(
            update = AvailableUpdate(
                version = "1.13.0",
                prerelease = false,
                notes = listOf(
                    VersionNotes("1.13.0", "### ✨ Added\n\n- The update prompt is a sheet."),
                    VersionNotes("1.12.0", "### 🐛 Fixed\n\n- Something that was broken."),
                ),
                pageUrl = "",
                apk = UpdateApk(name = "katari-v1.13.0.apk", url = "", size = 64_620_723, sha256 = null),
            ),
            status = AppUpdateStatus.Downloading(0.42f),
            onUpdate = {},
            onCancelDownload = {},
            onAllowInstalls = {},
            onSkipVersion = {},
            onOpenReleasePage = {},
            onDismiss = {},
        )
    }
}
