package eu.kanade.presentation.browse.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.model.InstalledForm
import eu.kanade.tachiyomi.extension.model.ListedDownload
import eu.kanade.tachiyomi.extension.store.isInstallable
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel
import eu.kanade.tachiyomi.util.system.LocaleHelper
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.Pill
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ExtensionItem(
    item: ExtensionUiModel.Item,
    onClickItem: (Extension) -> Unit,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onClickItemError: (InstallStep.Error) -> Unit,
    onClickItemAction: (Extension) -> Unit,
    onClickItemSecondaryAction: (Extension) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (extension, installStep) = item
    val store = when (extension) {
        is Extension.Available -> extension.store
        is Extension.Installed -> extension.store
    }
    SettingsRow(
        title = extension.name,
        modifier = modifier,
        subtitle = store?.name,
        subtitleMaxLines = 1,
        leading = { ExtensionItemIcon(extension = extension, installStep = installStep) },
        trailing = {
            ExtensionItemActions(
                extension = extension,
                installStep = installStep,
                onClickItemCancel = onClickItemCancel,
                onClickItemError = onClickItemError,
                onClickItemAction = onClickItemAction,
                onClickItemSecondaryAction = onClickItemSecondaryAction,
            )
        },
        onClick = { onClickItem(extension) },
        onLongClick = { onLongClickItem(extension) },
        below = { ExtensionItemDetails(extension = extension, installStep = installStep) },
    )
}

/** The extension's icon, shrunk inside a spinner while it is being installed. */
@Composable
private fun ExtensionItemIcon(
    extension: Extension,
    installStep: InstallStep,
) {
    val idle = installStep.isCompleted()
    val scale by animateFloatAsState(targetValue = if (idle) 1f else INSTALLING_ICON_SCALE, label = "iconScale")
    Box(contentAlignment = Alignment.Center) {
        if (!idle) {
            LoadingIndicator(modifier = Modifier.matchParentSize())
        }
        ExtensionIcon(
            extension = extension,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        )
    }
}

/** How small an icon gets inside its install spinner, so the spinner's ring clears it. */
private const val INSTALLING_ICON_SCALE = 0.6f

/** Language, version and install progress, separated by dots, then any warnings as pills. */
@Composable
private fun ExtensionItemDetails(
    extension: Extension,
    installStep: InstallStep,
) {
    val details = buildList {
        if (extension is Extension.Loaded && extension.lang.isNotEmpty()) {
            add(LocaleHelper.getSourceDisplayName(extension.lang, LocalContext.current))
        }
        if (extension.versionName.isNotEmpty()) {
            add(extension.versionName)
        }
        if (extension is Extension.Loaded && (extension.form as? InstalledForm.Package)?.isShared == false) {
            add(stringResource(MR.strings.ext_installer_private))
        }
        when {
            !installStep.isCompleted() -> {
                val progress = when (installStep) {
                    InstallStep.Pending -> MR.strings.ext_pending
                    InstallStep.Downloading -> MR.strings.ext_downloading
                    InstallStep.Installing -> MR.strings.ext_installing
                    else -> error("Must not show non-install process text")
                }
                add(stringResource(progress))
            }
            installStep == InstallStep.RequiresUserAction -> {
                add(stringResource(MR.strings.ext_requires_user_action))
            }
        }
    }
    val warnings = listOfNotNull(
        (installStep as? InstallStep.Error)?.let { MR.strings.ext_install_error to ContentTone.Error },
        when {
            extension is Extension.NotLoaded -> extension.reason.labelRes?.let { it to ContentTone.Error }
            extension is Extension.Loaded && extension.isObsolete -> MR.strings.ext_obsolete to ContentTone.Warning
            extension is Extension.Available && extension.isTorrent -> MR.strings.ext_torrents_only to ContentTone.Error
            // What else a store lists that can't be installed was built with a library version this app doesn't run
            extension is Extension.Available && !extension.isInstallable ->
                MR.strings.ext_unsupported_sdk to ContentTone.Error
            else -> null
        },
        (extension as? Extension.Loaded)?.sourceConflicts?.takeIf { it.isNotEmpty() }
            ?.let { MR.strings.ext_source_conflict to ContentTone.Warning },
        (MR.strings.ext_not_signed to ContentTone.Secondary).takeIf { extension.isFile },
        extension.contentWarning.label?.takeIf { it.onRows }?.let { it.title to it.tone },
    )
    // Won't look good but it's not like we can ellipsize overflowing content
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        details.forEachIndexed { index, text ->
            if (index > 0) Text(text = "•", role = TextRole.Supporting)
            Text(
                text = text,
                role = TextRole.Supporting,
                tone = ContentTone.Secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        warnings.forEach { (label, tone) ->
            Pill(
                text = stringResource(label),
                container = when (tone) {
                    ContentTone.Error -> SurfaceContainer.ErrorContainer
                    ContentTone.Warning -> SurfaceContainer.WarningContainer
                    else -> SurfaceContainer.SecondaryContainer
                },
                role = TextRole.Badge,
            )
        }
    }
}

/** A file extension, which nobody signs: its store vouches for it. */
private val Extension.isFile: Boolean
    get() = when (this) {
        is Extension.Available -> download is ListedDownload.File
        is Extension.Installed -> form is InstalledForm.File
    }

/**
 * Only the reasons the user can act on are worth naming in the row; the rest all mean "broken" to
 * them and are spelled out in [ExtensionNotLoadedDialog] instead.
 */
private val Extension.NotLoaded.Reason.labelRes: StringResource?
    get() = when (this) {
        is Extension.NotLoaded.Reason.Untrusted -> MR.strings.ext_untrusted
        Extension.NotLoaded.Reason.Filtered -> MR.strings.ext_filtered
        is Extension.NotLoaded.Reason.SourceConflicts -> MR.strings.ext_source_conflict
        // Nothing the user can do about it, but unlike a broken extension, updating won't change it
        Extension.NotLoaded.Reason.Torrent -> MR.strings.ext_torrents_only
        // Updating the extension or the app fixes it, and the dialog says which
        is Extension.NotLoaded.Reason.UnsupportedLibVersion -> MR.strings.ext_unsupported_sdk
        // Reinstalling fixes it
        Extension.NotLoaded.Reason.Damaged -> MR.strings.ext_damaged
        // The section header already says these aren't loaded; the dialog says why
        Extension.NotLoaded.Reason.Unsigned,
        Extension.NotLoaded.Reason.Malformed,
        Extension.NotLoaded.Reason.Unreadable,
        is Extension.NotLoaded.Reason.Failed,
        -> null
    }
