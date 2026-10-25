package eu.kanade.presentation.more.settings.screen.animeplayer

import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import eu.kanade.presentation.more.settings.widget.preferenceHighlight
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonSize
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.theme.KatariTheme
import mihon.video.engine.mpv.addon.MpvAddon
import mihon.video.engine.mpv.addon.MpvAddonPackage
import mihon.video.engine.mpv.addon.MpvAddonProblem
import mihon.video.engine.mpv.addon.MpvAddonState
import mihon.video.engine.mpv.addon.isFixedByUpdate
import mihon.video.engine.mpv.install.MpvAddonInstallState
import mihon.video.engine.mpv.install.MpvAddonInstallStatus
import mihon.video.engine.mpv.install.MpvAddonInstallation
import mihon.video.engine.mpv.install.describe
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The mpv add-on through its lifetime: not installed, downloading, installed, waiting for a restart, or needing an
 * update. It is the same for every profile, so it carries no profile chip.
 */
@Composable
internal fun MpvAddonSettingsRow(
    addon: MpvAddon,
    installation: MpvAddonInstallation,
    onRestart: () -> Unit,
) {
    val context = LocalContext.current
    val addonState by addon.state.collectAsState()
    val install by installation.state.collectAsState()
    // Installs and removals done in Android's own screens arrive as package events, but a screen coming back may have
    // missed them.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { addon.refresh() }
    val needsOffer = addonState == MpvAddonState.NotInstalled ||
        (addonState as? MpvAddonState.Installed)?.problem?.isFixedByUpdate == true
    LaunchedEffect(needsOffer) {
        if (needsOffer && install.offer == null && install.status == MpvAddonInstallStatus.Idle) installation.lookUp()
    }

    val row = mpvAddonRow(context, addonState, install, installation, onRestart)
    SettingsRow(
        title = stringResource(MR.strings.mpv_addon),
        modifier = Modifier.preferenceHighlight(),
        subtitle = row.subtitle,
        trailing = row.action?.let { action ->
            {
                when (action) {
                    is RowAction.Labelled -> Button(
                        label = action.label,
                        onClick = action.onClick,
                        style = action.style,
                        size = ButtonSize.Small,
                        enabled = action.enabled,
                    )
                    is RowAction.Uninstall -> IconButton(
                        icon = MaterialSymbols.Rounded.Delete,
                        contentDescription = stringResource(MR.strings.action_uninstall),
                        onClick = { context.startActivity(uninstallIntent()) },
                    )
                }
            }
        },
        below = (install.status as? MpvAddonInstallStatus.Downloading)?.let { downloading ->
            {
                LinearProgressIndicator(
                    progress = downloading.progress?.let { progress -> { progress } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = KatariTheme.spacing.s),
                )
            }
        },
    )
}

private data class RowContent(val subtitle: String, val action: RowAction?)

private sealed interface RowAction {
    data class Labelled(
        val label: String,
        val onClick: () -> Unit,
        val style: ButtonStyle = ButtonStyle.Tonal,
        val enabled: Boolean = true,
    ) : RowAction

    data object Uninstall : RowAction
}

@Composable
private fun mpvAddonRow(
    context: Context,
    addonState: MpvAddonState,
    install: MpvAddonInstallState,
    installation: MpvAddonInstallation,
    onRestart: () -> Unit,
): RowContent {
    val downloadSize = install.offer?.let { Formatter.formatShortFileSize(context, it.downloadBytes) }
    when (val status = install.status) {
        is MpvAddonInstallStatus.Downloading -> {
            val total = install.offer?.downloadBytes ?: 0L
            val progress = status.progress
            val subtitle = if (progress != null && total > 0) {
                stringResource(
                    MR.strings.mpv_addon_downloading,
                    Formatter.formatShortFileSize(context, (progress * total).toLong()),
                    Formatter.formatShortFileSize(context, total),
                )
            } else {
                stringResource(MR.strings.download_state_downloading)
            }
            return RowContent(
                subtitle,
                RowAction.Labelled(stringResource(MR.strings.action_cancel), installation::cancel, ButtonStyle.Text),
            )
        }
        MpvAddonInstallStatus.NeedsPermission -> return RowContent(
            stringResource(MR.strings.app_update_waiting_permission),
            RowAction.Labelled(
                stringResource(MR.strings.app_update_allow_installs),
                { context.startActivity(installation.installPermissionIntent()) },
            ),
        )
        MpvAddonInstallStatus.Installing -> return RowContent(stringResource(MR.strings.app_update_installing), null)
        is MpvAddonInstallStatus.Failed -> return RowContent(
            status.error.describe(context),
            RowAction.Labelled(stringResource(MR.strings.action_retry), installation::install),
        )
        MpvAddonInstallStatus.LookingUp, MpvAddonInstallStatus.Idle, MpvAddonInstallStatus.Installed -> Unit
    }
    return when (addonState) {
        MpvAddonState.Unknown -> RowContent(stringResource(MR.strings.mpv_addon_summary), null)
        MpvAddonState.NotInstalled -> RowContent(
            listOfNotNull(stringResource(MR.strings.mpv_addon_summary), downloadSize).joinToString(" · "),
            RowAction.Labelled(stringResource(MR.strings.action_install), installation::install),
        )
        is MpvAddonState.Installed -> when {
            addonState.restartNeeded -> RowContent(
                stringResource(MR.strings.mpv_addon_restart_needed, addonState.versionName),
                RowAction.Labelled(stringResource(MR.strings.mpv_addon_restart), onRestart),
            )
            addonState.problem == MpvAddonProblem.ForeignSigner -> RowContent(
                stringResource(MR.strings.mpv_addon_foreign_signer),
                RowAction.Uninstall,
            )
            addonState.problem != null -> RowContent(
                listOfNotNull(
                    install.offer?.let { stringResource(MR.strings.mpv_addon_update_needed, it.version) }
                        ?: stringResource(MR.strings.mpv_addon_update_needed_unknown),
                    downloadSize,
                ).joinToString(" · "),
                RowAction.Labelled(stringResource(MR.strings.action_update), installation::install),
            )
            else -> RowContent(
                stringResource(
                    MR.strings.mpv_addon_installed,
                    addonState.versionName,
                    Formatter.formatShortFileSize(context, addonState.sizeBytes),
                ),
                RowAction.Uninstall,
            )
        }
    }
}

/** Android's own uninstall dialog; Katari is the add-on's installer, but removing it is the user's call there. */
private fun uninstallIntent(): Intent = Intent(Intent.ACTION_DELETE, "package:${MpvAddonPackage.NAME}".toUri())
