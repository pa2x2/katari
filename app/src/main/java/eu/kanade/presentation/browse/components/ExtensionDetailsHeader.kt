package eu.kanade.presentation.browse.components

import android.util.DisplayMetrics
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
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.copyToClipboard
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.icons.materialsymbols.rounded.Eyeglasses2
import mihon.icons.materialsymbols.rounded.Info
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.LinkText
import mihon.presentation.kit.content.LogoSize
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.list.ToggleRow
import mihon.presentation.kit.surface.InfoTile
import mihon.presentation.kit.surface.InfoTileRow
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The top of an extension's details: its identity (tapping it copies debug information), version, language and
 * content warning as tiles, the uninstall and app info actions, and its incognito switch in a card.
 */
@Composable
internal fun ExtensionDetailsHeader(
    extension: Extension.Installed,
    extIncognitoMode: Boolean,
    onClickStore: (() -> Unit)?,
    onClickContentWarning: () -> Unit,
    onClickUninstall: () -> Unit,
    onClickAppInfo: (() -> Unit)?,
    onExtIncognitoChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val contentWarning = extension.contentWarning.label
    val spacing = KatariTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.m)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    context.copyToClipboard("Extension Debug information", extension.debugInfo())
                }
                .padding(horizontal = spacing.contentInset, vertical = spacing.s),
            horizontalArrangement = Arrangement.spacedBy(spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExtensionIcon(
                extension = extension,
                size = LogoSize.Header,
                density = DisplayMetrics.DENSITY_XXXHIGH,
            )
            Column(horizontalAlignment = Alignment.Start) {
                Text(text = extension.name, role = TextRole.DetailTitle)
                Text(
                    text = extension.pkgName.substringAfter("eu.kanade.tachiyomi.extension."),
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
