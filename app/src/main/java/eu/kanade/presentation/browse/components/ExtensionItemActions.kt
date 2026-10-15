package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.browse.extension.supportedEntryTypesForDisplay
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.rounded.OpenInBrowser
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.icons.materialsymbols.rounded.Settings
import mihon.icons.materialsymbols.rounded.VerifiedUser
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ExtensionItemActions(
    extension: Extension,
    installStep: InstallStep,
    modifier: Modifier = Modifier,
    onClickItemCancel: (Extension) -> Unit = {},
    onClickItemError: (InstallStep.Error) -> Unit = {},
    onClickItemAction: (Extension) -> Unit = {},
    onClickItemSecondaryAction: (Extension) -> Unit = {},
) {
    val isIdle = installStep.isCompleted()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourceEntryTypeIndicators(extension.supportedEntryTypesForDisplay())
        when {
            !isIdle -> {
                IconButton(
                    icon = MaterialSymbols.Rounded.Close,
                    contentDescription = stringResource(MR.strings.action_cancel),
                    onClick = { onClickItemCancel(extension) },
                )
            }
            installStep is InstallStep.Error -> {
                IconButton(
                    icon = MaterialSymbols.Rounded.Info,
                    contentDescription = stringResource(MR.strings.ext_install_error_details),
                    onClick = { onClickItemError(installStep) },
                )
                IconButton(
                    icon = MaterialSymbols.Rounded.Refresh,
                    contentDescription = stringResource(MR.strings.action_retry),
                    onClick = { onClickItemAction(extension) },
                )
            }
            installStep == InstallStep.RequiresUserAction -> {
                IconButton(
                    icon = MaterialSymbols.Rounded.Download,
                    contentDescription = stringResource(MR.strings.ext_update),
                    onClick = { onClickItemAction(extension) },
                )
            }
            installStep == InstallStep.Idle -> {
                when (extension) {
                    is Extension.Loaded -> {
                        IconButton(
                            icon = MaterialSymbols.Rounded.Settings,
                            contentDescription = stringResource(MR.strings.action_settings),
                            onClick = { onClickItemSecondaryAction(extension) },
                        )

                        if (extension.hasUpdate) {
                            IconButton(
                                icon = MaterialSymbols.Rounded.Download,
                                contentDescription = stringResource(MR.strings.ext_update),
                                onClick = { onClickItemAction(extension) },
                            )
                        }
                    }
                    is Extension.NotLoaded -> {
                        val isUntrusted = extension.reason is Extension.NotLoaded.Reason.Untrusted
                        IconButton(
                            icon = if (isUntrusted) {
                                MaterialSymbols.Rounded.VerifiedUser
                            } else {
                                MaterialSymbols.Rounded.Info
                            },
                            contentDescription = if (isUntrusted) {
                                stringResource(MR.strings.ext_trust)
                            } else {
                                stringResource(MR.strings.ext_not_loaded_details)
                            },
                            onClick = { onClickItemSecondaryAction(extension) },
                        )

                        if (extension.hasUpdate) {
                            IconButton(
                                icon = MaterialSymbols.Rounded.Download,
                                contentDescription = stringResource(MR.strings.ext_update),
                                onClick = { onClickItemAction(extension) },
                            )
                        }
                    }
                    is Extension.Available -> {
                        if (extension.sources.isNotEmpty()) {
                            IconButton(
                                icon = MaterialSymbols.Rounded.OpenInBrowser,
                                contentDescription = stringResource(MR.strings.action_open_in_web_view),
                                onClick = { onClickItemSecondaryAction(extension) },
                            )
                        }

                        IconButton(
                            icon = MaterialSymbols.Rounded.Download,
                            contentDescription = stringResource(MR.strings.ext_install),
                            onClick = { onClickItemAction(extension) },
                        )
                    }
                }
            }
        }
    }
}
