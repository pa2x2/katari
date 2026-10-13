package mihon.tts.ui.picker.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.VolumeUp
import mihon.icons.materialsymbols.rounded.Stop

@Composable
internal fun TtsRadioRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit,
    enabled: Boolean = true,
    playing: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (playing) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Stop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        } else if (selected) {
            Icon(
                imageVector = MaterialSymbols.AutoMirroredRounded.VolumeUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        RadioButton(selected = selected, enabled = enabled, onClick = null)
    }
}
