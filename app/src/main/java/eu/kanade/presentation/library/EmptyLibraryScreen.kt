package eu.kanade.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.secondaryItemAlpha

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
            modifier = Modifier
                .padding(top = 8.dp)
                .secondaryItemAlpha(),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        Column(
            modifier = Modifier
                .padding(top = 24.dp)
                .widthIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onBrowseSources, modifier = Modifier.fillMaxWidth()) {
                ButtonIcon(Icons.Outlined.Explore)
                Text(stringResource(MR.strings.action_browse_sources))
            }
            OutlinedButton(onClick = onRestoreBackup, modifier = Modifier.fillMaxWidth()) {
                ButtonIcon(Icons.Outlined.SettingsBackupRestore)
                Text(stringResource(MR.strings.pref_restore_backup))
            }
        }
        TextButton(onClick = onOpenGuide, modifier = Modifier.padding(top = 8.dp)) {
            ButtonIcon(Icons.AutoMirrored.Outlined.HelpOutline)
            Text(stringResource(MR.strings.getting_started_guide))
        }
    }
}

@Composable
private fun ButtonIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier
            .padding(end = ButtonDefaults.IconSpacing)
            .size(ButtonDefaults.IconSize),
    )
}
