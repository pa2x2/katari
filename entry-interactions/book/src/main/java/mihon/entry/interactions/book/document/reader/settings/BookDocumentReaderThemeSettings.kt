package mihon.entry.interactions.book.document.reader.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mihon.entry.interactions.book.R
import mihon.entry.viewer.settings.ViewerSettingBinding

@Composable
internal fun BookDocumentReaderThemeSettings(
    binding: ViewerSettingBinding<BookDocumentReaderThemeMode>,
) {
    val setting by binding.state.collectAsState()
    val scope = rememberCoroutineScope()
    val profileDefault = setting.profileValue ?: setting.processorDefault
    Column {
        Text(
            text = stringResource(R.string.book_document_reader_theme),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        BookDocumentReaderThemeMode.entries.forEach { mode ->
            ThemeSettingRow(
                label = mode.label(),
                selected = setting.effectiveValue == mode,
                onClick = {
                    scope.launch {
                        if (mode == profileDefault) {
                            binding.clearEntryOverride()
                        } else {
                            binding.setEntryOverride(mode)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ThemeSettingRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label)
    }
}

@Composable
private fun BookDocumentReaderThemeMode.label(): String = stringResource(
    when (this) {
        BookDocumentReaderThemeMode.APP -> R.string.book_document_reader_theme_app
        BookDocumentReaderThemeMode.BLACK -> R.string.book_document_reader_theme_black
    },
)
