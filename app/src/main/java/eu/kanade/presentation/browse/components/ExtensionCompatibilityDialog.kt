package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.extension.kind.ExtensionFamily
import mihon.presentation.kit.content.LogoFrame
import mihon.presentation.kit.content.LogoSize
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.surface.LetterTile
import mihon.presentation.kit.surface.Pill
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The extension libraries this build loads and the versions of each, which is what decides what it can install. */
@Composable
internal fun ExtensionCompatibilityDialog(onDismissRequest: () -> Unit) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.ext_compatibility),
        confirm = DialogAction(stringResource(MR.strings.action_ok), onDismissRequest),
    ) {
        Column {
            ExtensionFamily.entries.forEach { family ->
                val name = stringResource(family.titleRes)
                SettingsRow(
                    title = name,
                    leading = {
                        LogoFrame(size = LogoSize.Row) {
                            LetterTile(name = name, modifier = Modifier.fillMaxSize())
                        }
                    },
                    below = { ExtensionFamilyFacts(family) },
                )
            }
        }
    }
}

@Composable
private fun ExtensionFamilyFacts(family: ExtensionFamily) {
    Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs)) {
            FactLabel(stringResource(MR.strings.ext_compatibility_built_with))
            Text(text = family.library, role = TextRole.Supporting)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            FactLabel(stringResource(MR.strings.ext_compatibility_versions))
            family.libVersions.forEach { version ->
                Pill(text = version, container = SurfaceContainer.PrimaryContainer, role = TextRole.Badge)
            }
        }
    }
}

@Composable
private fun FactLabel(text: String) {
    Text(text = text, role = TextRole.Supporting, tone = ContentTone.Secondary)
}
