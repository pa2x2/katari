package eu.kanade.presentation.browse.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.katari.core.platform.currentPlatformContext
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Delete
import app.katari.icons.materialsymbols.rounded.Eyeglasses2
import app.katari.icons.materialsymbols.rounded.Info
import app.katari.icons.materialsymbols.rounded.Lock
import app.katari.icons.materialsymbols.rounded.OpenInBrowser
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.LinkText
import app.katari.presentation.kit.content.LogoSize
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.list.ToggleRow
import app.katari.presentation.kit.surface.InfoTile
import app.katari.presentation.kit.surface.InfoTileRow
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstalledForm
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The top of an extension's details: its identity (tapping it copies debug information), version, language and
 * content warning as tiles, the uninstall and app info or website actions, and its incognito switch in a card. A file
 * extension also says in a card that nobody signed it and how its file is checked instead.
 */
@Composable
internal fun ExtensionDetailsHeader(
    extension: Extension.Installed,
    extIncognitoMode: Boolean,
    onClickStore: (() -> Unit)?,
    onClickContentWarning: () -> Unit,
    onClickUninstall: () -> Unit,
    onClickAppInfo: (() -> Unit)?,
    onClickWebsite: (() -> Unit)?,
    onExtIncognitoChange: (Boolean) -> Unit,
) {
    val context = currentPlatformContext()
    val graph = LocalAppUiGraph.current
    val sourceRows = graph.sourceRows
    val textClipboard = graph.textClipboard
    val contentWarning = extension.contentWarning.label
    val spacing = KatariTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.m)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    textClipboard.copy("Extension Debug information", extension.debugInfo())
                }
                .padding(horizontal = spacing.contentInset, vertical = spacing.s),
            horizontalArrangement = Arrangement.spacedBy(spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            sourceRows.ExtensionIcon(extension = extension, size = LogoSize.Header, modifier = Modifier)
            Column(horizontalAlignment = Alignment.Start) {
                Text(text = extension.name, role = TextRole.DetailTitle)
                Text(
                    text = when (extension.form) {
                        is InstalledForm.Package -> extension.pkgName.substringAfter("eu.kanade.tachiyomi.extension.")
                        is InstalledForm.File -> extension.pkgName.replaceFirst(":", " · ")
                    },
                    role = TextRole.Supporting,
                    tone = ContentTone.Secondary,
                )
                extension.store?.let { store ->
                    LinkText(text = store.name, onClick = onClickStore)
                }
            }
        }

        InfoTileRow {
            InfoTile(value = extension.versionName, label = stringResource(MR.strings.ext_info_version))
            InfoTile(
                value = LocaleHelper.getSourceDisplayName(extension.lang, context),
                label = stringResource(MR.strings.ext_info_language),
            )
            if (contentWarning != null) {
                InfoTile(
                    value = stringResource(contentWarning.title),
                    label = stringResource(MR.strings.ext_info_warning),
                    valueTone = contentWarning.tone,
                    onClick = onClickContentWarning,
                )
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = spacing.m),
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
        ) {
            Button(
                label = stringResource(MR.strings.ext_uninstall),
                onClick = onClickUninstall,
                modifier = Modifier.weight(1f),
                style = ButtonStyle.Tonal,
                icon = MaterialSymbols.Rounded.Delete,
            )
            if (onClickAppInfo != null) {
                Button(
                    label = stringResource(MR.strings.ext_app_info),
                    onClick = onClickAppInfo,
                    modifier = Modifier.weight(1f),
                    style = ButtonStyle.Tonal,
                    icon = MaterialSymbols.Rounded.Info,
                )
            }
            if (onClickWebsite != null) {
                Button(
                    label = stringResource(MR.strings.website),
                    onClick = onClickWebsite,
                    modifier = Modifier.weight(1f),
                    style = ButtonStyle.Tonal,
                    icon = MaterialSymbols.Rounded.OpenInBrowser,
                )
            }
        }

        (extension.form as? InstalledForm.File)?.let { form ->
            RowGroupItem(position = RowGroupPosition.Alone) {
                SettingsRow(
                    title = stringResource(MR.strings.ext_not_signed),
                    subtitle = notSignedSummary(extension.store?.name, form.sha256),
                    icon = MaterialSymbols.Rounded.Lock,
                )
            }
        }

        RowGroupItem(position = RowGroupPosition.Alone) {
            ToggleRow(
                title = stringResource(MR.strings.pref_incognito_mode),
                subtitle = stringResource(MR.strings.pref_incognito_mode_extension_summary),
                icon = MaterialSymbols.Rounded.Eyeglasses2,
                checked = extIncognitoMode,
                onCheckedChange = onExtIncognitoChange,
            )
        }
    }
}

@Composable
private fun notSignedSummary(storeName: String?, sha256: String): String {
    val hash = "${sha256.take(HASH_SHOWN)}…${sha256.takeLast(HASH_SHOWN)}"
    return if (storeName != null) {
        stringResource(MR.strings.ext_not_signed_summary, storeName, hash)
    } else {
        stringResource(MR.strings.ext_not_signed_summary_no_store, hash)
    }
}

// Enough of each end to compare by eye, as Git shortens hashes
private const val HASH_SHOWN = 4

private fun Extension.Installed.debugInfo(): String = buildString {
    append(
        """
        Extension name: $name (lang: $lang; package: $pkgName)
        Extension version: $versionName (lib: $libVersion; version code: $versionCode)
        Content warning: $contentWarning
        """.trimIndent(),
    )

    if (this@debugInfo is Extension.Loaded) {
        append("\n\n")
        appendLine(
            """
            Update available: $hasUpdate
            Orphaned: $isObsolete
            Installed as: $form
            """.trimIndent(),
        )
        val store = store
        if (store != null) {
            append("Repository: ${store.indexUrl}")
        }
    }
}
