package mihon.feature.appupdate.mpvaddon.ui

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import mihon.feature.appupdate.check.UpdateApk
import mihon.feature.appupdate.mpvaddon.MpvAddonRelease
import mihon.feature.appupdate.mpvaddon.MpvAddonUpdate
import mihon.feature.appupdate.ui.UpdateSheetDetail
import mihon.feature.appupdate.ui.UpdateSheetFrame
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.sheet.SheetActions
import mihon.presentation.kit.theme.KatariPreviewTheme
import mihon.presentation.kit.theme.TextRole
import mihon.video.engine.mpv.install.MpvAddonInstallStatus
import mihon.video.engine.mpv.install.describe
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The add-on's own update sheet, for when only the add-on is behind this Katari. A required update has no "Skip this
 * version": without it mpv doesn't run with this Katari.
 */
@Composable
internal fun MpvAddonUpdateSheet(
    update: MpvAddonUpdate,
    status: MpvAddonInstallStatus,
    onUpdate: () -> Unit,
    onCancelDownload: () -> Unit,
    onAllowInstalls: () -> Unit,
    onSkipVersion: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val release = update.release
    UpdateSheetFrame(
        title = stringResource(MR.strings.mpv_addon_update_title),
        summary = listOfNotNull(
            stringResource(MR.strings.app_update_version, release.version),
            Formatter.formatShortFileSize(context, release.apk.size),
            stringResource(MR.strings.mpv_addon_update_required).takeIf { update.required },
        ).joinToString(" · "),
        detail = when (status) {
            is MpvAddonInstallStatus.Downloading -> UpdateSheetDetail.Progress(status.progress)
            MpvAddonInstallStatus.NeedsPermission ->
                UpdateSheetDetail.Note(stringResource(MR.strings.app_update_permission_note))
            is MpvAddonInstallStatus.Failed -> UpdateSheetDetail.Error(status.error.describe(context))
            else -> null
        },
        onOpenReleasePage = null,
        onDismiss = onDismiss,
        actions = {
            val notNow = DialogAction(stringResource(MR.strings.action_not_now), onDismiss)
            when (status) {
                is MpvAddonInstallStatus.Downloading -> SheetActions(
                    confirm = null,
                    dismiss = DialogAction(stringResource(MR.strings.app_update_cancel_download), onCancelDownload),
                )
                MpvAddonInstallStatus.LookingUp -> SheetActions(
                    confirm = DialogAction(stringResource(MR.strings.app_update_checking), {}, enabled = false),
                )
                MpvAddonInstallStatus.Installing -> SheetActions(
                    confirm = DialogAction(stringResource(MR.strings.app_update_installing), {}, enabled = false),
                )
                MpvAddonInstallStatus.NeedsPermission -> SheetActions(
                    confirm = DialogAction(stringResource(MR.strings.app_update_allow_installs), onAllowInstalls),
                    dismiss = notNow,
                )
                is MpvAddonInstallStatus.Failed -> SheetActions(
                    confirm = DialogAction(stringResource(MR.strings.action_retry), onUpdate),
                    dismiss = notNow,
                )
                else -> SheetActions(
                    confirm = DialogAction(stringResource(MR.strings.app_update_now), onUpdate),
                    dismiss = notNow,
                    neutral = DialogAction(stringResource(MR.strings.app_update_skip_version), onSkipVersion)
                        .takeIf { !update.required },
                )
            }
        },
        notes = release.components?.let { components ->
            {
                Text(
                    text = listOf(components, stringResource(MR.strings.mpv_addon_summary)).joinToString("\n"),
                    role = TextRole.Body,
                )
            }
        },
    )
}

@PreviewLightDark
@Composable
private fun MpvAddonUpdateSheetPreview() {
    KatariPreviewTheme {
        MpvAddonUpdateSheet(
            update = MpvAddonUpdate(
                release = MpvAddonRelease(
                    version = "2026-11-02-1",
                    versionCode = 2026110201,
                    glue = 1,
                    apk = UpdateApk(name = "katari-mpv-arm64-v8a-2026-11-02-1.apk", url = "", size = 13_210_829, null),
                    components = "mpv 0.41.0, FFmpeg 9.0.2",
                ),
                required = true,
            ),
            status = MpvAddonInstallStatus.Idle,
            onUpdate = {},
            onCancelDownload = {},
            onAllowInstalls = {},
            onSkipVersion = {},
            onDismiss = {},
        )
    }
}
