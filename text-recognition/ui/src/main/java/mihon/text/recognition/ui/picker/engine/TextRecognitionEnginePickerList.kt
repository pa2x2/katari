package mihon.text.recognition.ui.picker.engine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.provider.KnownTextRecognitionProvider
import mihon.text.recognition.api.provider.TextRecognitionBuildAvailability
import mihon.text.recognition.api.provider.TextRecognitionProviderId
import mihon.text.recognition.ui.language.displayNames
import mihon.text.recognition.ui.settings.isIncluded
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Engine cards; engines this build excludes are listed but cannot be chosen. */
@Composable
fun TextRecognitionEnginePickerList(
    providers: List<KnownTextRecognitionProvider>,
    components: List<KnownTextRecognitionComponent>,
    selected: TextRecognitionProviderId?,
    onSelect: (TextRecognitionProviderId) -> Unit,
    onOpenDocumentation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var details by remember { mutableStateOf<KnownTextRecognitionProvider?>(null) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(providers, key = { it.id.value }) { provider ->
            TextRecognitionEngineCard(
                provider = provider,
                languages = components.filter { it.provider == provider.id }.flatMap { it.languages }.distinct(),
                selected = provider.id == selected,
                onSelect = { onSelect(provider.id) },
                onOpenDetails = { details = provider },
            )
        }
    }
    details?.let { provider ->
        TextRecognitionEngineDetailsSheet(
            provider = provider,
            components = components.filter { it.provider == provider.id },
            onOpenDocumentation = onOpenDocumentation,
            onDismiss = { details = null },
        )
    }
}

@Composable
private fun TextRecognitionEngineCard(
    provider: KnownTextRecognitionProvider,
    languages: List<LanguageTag>,
    selected: Boolean,
    onSelect: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    val included = provider.isIncluded
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = included, role = Role.RadioButton, onClick = onSelect),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(provider.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = provider.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RadioButton(selected = selected, enabled = included, onClick = null)
            }
            TextRecognitionEngineStatusPill(provider.buildAvailability)
            when (val availability = provider.buildAvailability) {
                TextRecognitionBuildAvailability.Included -> Text(
                    text = stringResource(MR.strings.text_recognition_engine_languages, languages.displayNames()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is TextRecognitionBuildAvailability.NotIncluded -> Text(
                    text = availability.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onOpenDetails) {
                Text(stringResource(MR.strings.text_recognition_engine_details))
            }
        }
    }
}

@Composable
private fun TextRecognitionEngineStatusPill(availability: TextRecognitionBuildAvailability) {
    val (label, colors) = when (availability) {
        TextRecognitionBuildAvailability.Included -> stringResource(
            MR.strings.text_recognition_engine_status_available,
        ) to
            (MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer)
        is TextRecognitionBuildAvailability.NotIncluded ->
            stringResource(MR.strings.text_recognition_engine_status_not_included) to
                (MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer)
    }
    Surface(shape = MaterialTheme.shapes.extraLarge, color = colors.first, contentColor = colors.second) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
