package mihon.feature.appupdate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import eu.kanade.presentation.components.MarkdownRender
import mihon.feature.appupdate.AppUpdateStatus
import mihon.feature.appupdate.check.AvailableUpdate
import mihon.feature.appupdate.check.UpdateApk
import mihon.feature.appupdate.check.VersionNotes
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.OpenInNew
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariPreviewTheme
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The update prompt: what the new version brings, then download and install progress, over whatever screen is open.
 * Swiping it away is "Not now". Long notes scroll inside their card, which takes what the capped sheet leaves after
 * the title and actions.
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
    Sheet(onDismissRequest = onDismiss, capHeight = true) {
        Column(modifier = Modifier.padding(top = KatariTheme.spacing.l, bottom = KatariTheme.spacing.m)) {
            Row(
                modifier = Modifier.padding(start = KatariTheme.spacing.rowInset, end = KatariTheme.spacing.xs),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(MR.strings.app_update_available), role = TextRole.SheetTitle)
                    Text(text = update.summary(), role = TextRole.Body, tone = ContentTone.Secondary)
                }
                IconButton(
                    icon = MaterialSymbols.AutoMirroredRounded.OpenInNew,
                    contentDescription = stringResource(MR.strings.update_check_open),
                    onClick = onOpenReleasePage,
                )
            }

            if (update.notes.isNotEmpty()) {
                ReleaseNotesCard(
                    notes = update.notes,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(
                            start = KatariTheme.spacing.rowInset,
                            end = KatariTheme.spacing.rowInset,
                            top = KatariTheme.spacing.m,
                        ),
                )
            }
            Column(
                modifier = Modifier.padding(
                    start = KatariTheme.spacing.rowInset,
                    end = KatariTheme.spacing.rowInset,
                    top = KatariTheme.spacing.m,
                ),
            ) {
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

/** Each version's notes under its own heading, newest first. */
@Composable
private fun ReleaseNotesCard(notes: List<VersionNotes>, modifier: Modifier = Modifier) {
    val flavour = remember { GFMFlavourDescriptor() }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(KatariTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        ) {
            notes.forEachIndexed { index, versionNotes ->
                // One version's notes speak for themselves; several need telling apart. The label matches the notes'
                // own ### headings in size, so its colour and the divider set it above them.
                if (notes.size > 1) {
                    if (index > 0) HorizontalDivider()
                    Text(text = versionNotes.version, role = TextRole.ScreenTitle, tone = ContentTone.Primary)
                }
                MarkdownRender(
                    content = versionNotes.notes,
                    flavour = flavour,
                    listBlockSpacing = KatariTheme.spacing.xs,
                    listItemSpacing = KatariTheme.spacing.xs,
                    listSectionSpacing = KatariTheme.spacing.s,
                )
            }
        }
    }
}

@Composable
private fun StatusDetail(status: AppUpdateStatus) {
    val text = when (status) {
        is AppUpdateStatus.Downloading -> {
            LinearProgressIndicator(
                progress = status.progress?.let { progress -> { progress } },
                modifier = Modifier.fillMaxWidth(),
            )
            downloadingText(status.progress)
        }
        AppUpdateStatus.NeedsPermission -> stringResource(MR.strings.app_update_permission_note)
        AppUpdateStatus.Installing -> stringResource(MR.strings.app_update_installing_note)
        is AppUpdateStatus.Failed -> status.error.message()
        else -> null
    } ?: return
    Text(
        text = text,
        role = TextRole.Body,
        tone = if (status is AppUpdateStatus.Failed) ContentTone.Error else ContentTone.Secondary,
        modifier = Modifier.padding(top = KatariTheme.spacing.s, bottom = KatariTheme.spacing.m),
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
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
    ) {
        val fullWidth = Modifier.fillMaxWidth()
        when (status) {
            is AppUpdateStatus.Downloading -> Button(
                label = stringResource(MR.strings.app_update_cancel_download),
                onClick = onCancelDownload,
                modifier = fullWidth,
                style = ButtonStyle.Tonal,
            )
            AppUpdateStatus.Installing -> Button(
                label = stringResource(MR.strings.app_update_installing),
                onClick = {},
                modifier = fullWidth,
                enabled = false,
                loading = true,
            )
            AppUpdateStatus.NeedsPermission -> Button(
                label = stringResource(MR.strings.app_update_allow_installs),
                onClick = onAllowInstalls,
                modifier = fullWidth,
            )
            is AppUpdateStatus.Failed -> Button(
                label = stringResource(MR.strings.action_retry),
                onClick = onUpdate,
                modifier = fullWidth,
            )
            else -> Button(label = stringResource(MR.strings.app_update_now), onClick = onUpdate, modifier = fullWidth)
        }
        if (status is AppUpdateStatus.Failed) {
            Button(
                label = stringResource(MR.strings.update_check_open),
                onClick = onOpenReleasePage,
                modifier = fullWidth,
                style = ButtonStyle.Text,
            )
        }
        if (status == AppUpdateStatus.Available) {
            Button(
                label = stringResource(MR.strings.app_update_skip_version),
                onClick = onSkipVersion,
                modifier = fullWidth,
                style = ButtonStyle.Text,
            )
        }
        if (status == AppUpdateStatus.Available ||
            status == AppUpdateStatus.NeedsPermission ||
            status is AppUpdateStatus.Failed
        ) {
            Button(
                label = stringResource(MR.strings.action_not_now),
                onClick = onDismiss,
                modifier = fullWidth,
                style = ButtonStyle.Text,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun AppUpdateSheetPreview() {
    KatariPreviewTheme {
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
