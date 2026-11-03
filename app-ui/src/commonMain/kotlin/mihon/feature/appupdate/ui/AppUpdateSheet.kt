package mihon.feature.appupdate.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.PreviewLightDark
import eu.kanade.presentation.components.MarkdownRender
import mihon.feature.appupdate.AppUpdateStatus
import mihon.feature.appupdate.check.AvailableUpdate
import mihon.feature.appupdate.check.GithubRelease
import mihon.feature.appupdate.check.UpdateAsset
import mihon.feature.appupdate.check.VersionNotes
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.sheet.SheetActions
import mihon.presentation.kit.surface.PaneDivider
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariPreviewTheme
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The update prompt: what the new version brings, then download and install progress, over whatever screen is open.
 * What the update brings along, such as Android's mpv add-on, goes last in the summary as [broughtAlongSummary] and
 * after the notes as [broughtAlongNotes].
 */
@Composable
fun AppUpdateSheet(
    update: AvailableUpdate,
    status: AppUpdateStatus,
    onUpdate: () -> Unit,
    onCancelDownload: () -> Unit,
    onAllowInstalls: () -> Unit,
    onRestart: () -> Unit,
    onSkipVersion: () -> Unit,
    onOpenReleasePage: () -> Unit,
    onDismiss: () -> Unit,
    broughtAlongSummary: String? = null,
    broughtAlongNotes: (@Composable () -> Unit)? = null,
) {
    UpdateSheetFrame(
        title = stringResource(MR.strings.app_update_available),
        summary = update.summary(broughtAlongSummary),
        detail = status.detail(),
        onOpenReleasePage = onOpenReleasePage,
        onDismiss = onDismiss,
        actions = {
            Actions(
                status = status,
                onUpdate = onUpdate,
                onCancelDownload = onCancelDownload,
                onAllowInstalls = onAllowInstalls,
                onRestart = onRestart,
                onSkipVersion = onSkipVersion,
                onOpenReleasePage = onOpenReleasePage,
                onDismiss = onDismiss,
            )
        },
        notes = if (update.notes.isNotEmpty() || broughtAlongNotes != null) {
            { ReleaseNotes(update.notes, broughtAlongNotes) }
        } else {
            null
        },
    )
}

/** Each version's notes under its own heading, newest first, then what the update brings along. */
@Composable
private fun ReleaseNotes(notes: List<VersionNotes>, broughtAlong: (@Composable () -> Unit)?) {
    val flavour = remember { GFMFlavourDescriptor() }
    val headed = notes.size > 1 || broughtAlong != null
    notes.forEachIndexed { index, versionNotes ->
        // One version's notes speak for themselves; several need telling apart. The label matches the notes' own ###
        // headings in size, so its colour and the divider set it above them.
        if (headed) {
            if (index > 0) PaneDivider()
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
    if (broughtAlong != null) {
        if (notes.isNotEmpty()) PaneDivider()
        broughtAlong()
    }
}

@Composable
private fun AppUpdateStatus.detail(): UpdateSheetDetail? = when (this) {
    is AppUpdateStatus.Downloading -> UpdateSheetDetail.Progress(progress)
    AppUpdateStatus.NeedsPermission -> UpdateSheetDetail.Note(stringResource(MR.strings.app_update_permission_note))
    AppUpdateStatus.Installing -> UpdateSheetDetail.Note(stringResource(MR.strings.app_update_installing_note))
    AppUpdateStatus.ReadyToRestart -> UpdateSheetDetail.Note(stringResource(MR.strings.app_update_restart_note))
    is AppUpdateStatus.Failed -> UpdateSheetDetail.Error(error.message())
    else -> null
}

@Composable
private fun Actions(
    status: AppUpdateStatus,
    onUpdate: () -> Unit,
    onCancelDownload: () -> Unit,
    onAllowInstalls: () -> Unit,
    onRestart: () -> Unit,
    onSkipVersion: () -> Unit,
    onOpenReleasePage: () -> Unit,
    onDismiss: () -> Unit,
) {
    val notNow = DialogAction(stringResource(MR.strings.action_not_now), onDismiss)
    when (status) {
        is AppUpdateStatus.Downloading -> SheetActions(
            confirm = null,
            dismiss = DialogAction(stringResource(MR.strings.app_update_cancel_download), onCancelDownload),
        )
        AppUpdateStatus.Installing -> SheetActions(
            confirm = DialogAction(stringResource(MR.strings.app_update_installing), onClick = {}, enabled = false),
        )
        AppUpdateStatus.NeedsPermission -> SheetActions(
            confirm = DialogAction(stringResource(MR.strings.app_update_allow_installs), onAllowInstalls),
            dismiss = notNow,
        )
        AppUpdateStatus.ReadyToRestart -> SheetActions(
            confirm = DialogAction(stringResource(MR.strings.action_restart), onRestart),
            dismiss = notNow,
        )
        is AppUpdateStatus.Failed -> SheetActions(
            confirm = DialogAction(stringResource(MR.strings.action_retry), onUpdate),
            dismiss = notNow,
            neutral = DialogAction(stringResource(MR.strings.update_check_open), onOpenReleasePage),
        )
        else -> SheetActions(
            confirm = DialogAction(stringResource(MR.strings.app_update_now), onUpdate),
            dismiss = notNow,
            neutral = DialogAction(stringResource(MR.strings.app_update_skip_version), onSkipVersion)
                .takeIf { status == AppUpdateStatus.Available },
        )
    }
}

@PreviewLightDark
@Composable
private fun AppUpdateSheetPreview() {
    KatariPreviewTheme {
        AppUpdateSheet(
            update = AvailableUpdate(
                release = GithubRelease(tagName = "v1.13.0", pageUrl = ""),
                version = "1.13.0",
                prerelease = false,
                asset = UpdateAsset(name = "katari-v1.13.0.apk", url = "", size = 64_620_723, sha256 = null),
                notes = listOf(
                    VersionNotes("1.13.0", "### ✨ Added\n\n- The update prompt is a sheet."),
                    VersionNotes("1.12.0", "### 🐛 Fixed\n\n- Something that was broken."),
                ),
            ),
            status = AppUpdateStatus.Downloading(0.42f),
            onUpdate = {},
            onCancelDownload = {},
            onAllowInstalls = {},
            onRestart = {},
            onSkipVersion = {},
            onOpenReleasePage = {},
            onDismiss = {},
        )
    }
}
