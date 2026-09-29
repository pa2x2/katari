package mihon.text.recognition.ui.picker.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.provider.KnownTextRecognitionProvider
import mihon.text.recognition.ui.language.displayNames
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Where an engine processes pages and which components it combines. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TextRecognitionEngineDetailsSheet(
    provider: KnownTextRecognitionProvider,
    components: List<KnownTextRecognitionComponent>,
    onOpenDocumentation: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(provider.name, style = MaterialTheme.typography.titleLarge)
            Text(provider.description, style = MaterialTheme.typography.bodyMedium)
            DetailsSection(stringResource(MR.strings.text_recognition_engine_processing), provider.processingLocation)
            Text(
                text = stringResource(MR.strings.text_recognition_engine_components),
                style = MaterialTheme.typography.titleSmall,
            )
            components.forEach { component ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(component.displayName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = component.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (component.languages.isNotEmpty()) {
                        Text(
                            text = stringResource(
                                MR.strings.text_recognition_engine_languages,
                                component.languages.displayNames(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    component.documentationUrl?.let { url ->
                        TextButton(onClick = { onOpenDocumentation(url) }) {
                            Text(stringResource(MR.strings.text_recognition_engine_documentation))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
