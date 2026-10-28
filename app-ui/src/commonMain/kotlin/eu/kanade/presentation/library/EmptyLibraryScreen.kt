package eu.kanade.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Help
import mihon.icons.materialsymbols.rounded.Explore
import mihon.icons.materialsymbols.rounded.SettingsBackupRestore
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A library with nothing in it yet: the two ways to fill it, adding from a source or restoring a backup. */
@Composable
fun EmptyLibraryScreen(
    onBrowseSources: () -> Unit,
    onRestoreBackup: () -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyScreen(
        stringRes = MR.strings.information_empty_library,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(MR.strings.information_empty_library_hint),
            modifier = Modifier.padding(top = KatariTheme.spacing.s),
            role = TextRole.Supporting,
            textAlign = TextAlign.Center,
        )
        // Both ways to fill the library share the width of the longer label.
        Column(
            modifier = Modifier
                .padding(top = KatariTheme.spacing.xl)
                .width(IntrinsicSize.Max),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            Button(
                label = stringResource(MR.strings.action_browse_sources),
                onClick = onBrowseSources,
                modifier = Modifier.fillMaxWidth(),
                icon = MaterialSymbols.Rounded.Explore,
            )
            Button(
                label = stringResource(MR.strings.pref_restore_backup),
                onClick = onRestoreBackup,
                modifier = Modifier.fillMaxWidth(),
                style = ButtonStyle.Outlined,
                icon = MaterialSymbols.Rounded.SettingsBackupRestore,
            )
        }
        Button(
            label = stringResource(MR.strings.getting_started_guide),
            onClick = onOpenGuide,
            modifier = Modifier.padding(top = KatariTheme.spacing.s),
            style = ButtonStyle.Text,
            icon = MaterialSymbols.AutoMirroredRounded.Help,
        )
    }
}
