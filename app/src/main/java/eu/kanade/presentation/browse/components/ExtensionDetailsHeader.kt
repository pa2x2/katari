package eu.kanade.presentation.browse.components

import android.util.DisplayMetrics
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.copyToClipboard
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Eyeglasses2
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.BrowseMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.ToggleRow
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.surface.VerticalDivider
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The top of an extension's details: its identity (tapping it copies debug information), version, language and
 * content warning, the uninstall and app info actions, and its incognito switch.
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

    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.contentInset)
                .padding(top = spacing.l, bottom = spacing.s)
                .clickable {
                    context.copyToClipboard("Extension Debug information", extension.debugInfo())
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ExtensionIcon(
                modifier = Modifier.size(BrowseMetrics.extensionHeaderIcon),
                extension = extension,
                density = DisplayMetrics.DENSITY_XXXHIGH,
            )

            Text(
                text = extension.name,
                role = TextRole.SheetTitle,
                textAlign = TextAlign.Center,
            )

            Text(
                text = extension.pkgName.substringAfter("eu.kanade.tachiyomi.extension."),
                role = TextRole.Supporting,
            )

            extension.store?.let { store ->
                Text(
                    text = store.name,
                    modifier = Modifier
                        .clip(KatariTheme.shapes.small)
                        .clickable(enabled = onClickStore != null) { onClickStore?.invoke() }
                        .padding(horizontal = spacing.xs, vertical = spacing.xxs),
                    role = TextRole.Label,
                    tone = ContentTone.Primary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.contentInset, vertical = spacing.s),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InfoText(
                modifier = Modifier.weight(1f),
                primaryText = extension.versionName,
                secondaryText = stringResource(MR.strings.ext_info_version),
            )

            InfoDivider()

            InfoText(
                modifier = Modifier.weight(1f),
                primaryText = LocaleHelper.getSourceDisplayName(extension.lang, context),
                secondaryText = stringResource(MR.strings.ext_info_language),
            )

            if (contentWarning != null) {
                InfoDivider()

                InfoText(
                    modifier = Modifier.weight(1f),
                    primaryText = stringResource(contentWarning.title),
                    primaryTone = contentWarning.tone,
                    secondaryText = stringResource(MR.strings.ext_info_warning),
                    onClick = onClickContentWarning,
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(horizontal = spacing.contentInset)
                .padding(top = spacing.s),
            horizontalArrangement = Arrangement.spacedBy(spacing.l),
        ) {
            Button(
                label = stringResource(MR.strings.ext_uninstall),
                onClick = onClickUninstall,
                modifier = Modifier.weight(1f),
                style = ButtonStyle.Outlined,
            )

            if (onClickAppInfo != null) {
                Button(
                    label = stringResource(MR.strings.ext_app_info),
                    onClick = onClickAppInfo,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        ToggleRow(
            title = stringResource(MR.strings.pref_incognito_mode),
            subtitle = stringResource(MR.strings.pref_incognito_mode_extension_summary),
            icon = MaterialSymbols.Rounded.Eyeglasses2,
            checked = extIncognitoMode,
            onCheckedChange = onExtIncognitoChange,
        )

        HorizontalDivider()
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
            Shared: $isShared
            """.trimIndent(),
        )
        val store = store
        if (store != null) {
            append("Repository: ${store.indexUrl}")
        }
    }
}

@Composable
private fun InfoText(
    primaryText: String,
    secondaryText: String,
    modifier: Modifier = Modifier,
    primaryTone: ContentTone = ContentTone.Inherit,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(interactionSource = null, indication = null, onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = modifier.then(clickableModifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = primaryText,
            role = TextRole.Label,
            tone = primaryTone,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = secondaryText + if (onClick != null) " ⓘ" else "",
            role = TextRole.Supporting,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun InfoDivider() {
    VerticalDivider(
        modifier = Modifier
            .padding(horizontal = KatariTheme.spacing.s)
            .height(KatariTheme.sizes.icon),
    )
}
