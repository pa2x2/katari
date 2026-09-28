package mihon.text.recognition.ui.picker.engine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.provider.KnownTextRecognitionProvider
import mihon.text.recognition.api.provider.TextRecognitionBuildAvailability
import mihon.text.recognition.api.provider.TextRecognitionProviderId
import mihon.text.recognition.ui.language.displayName
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
                components = components.filter { it.provider == provider.id },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextRecognitionEngineCard(
    provider: KnownTextRecognitionProvider,
    components: List<KnownTextRecognitionComponent>,
    selected: Boolean,
    onSelect: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    val included = provider.isIncluded
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (included) 1f else EXCLUDED_ALPHA)
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
                if (included) {
                    RadioButton(selected = selected, onClick = null)
                } else {
                    EnginePill(stringResource(MR.strings.text_recognition_engine_status_not_included))
                }
            }
            when (val availability = provider.buildAvailability) {
                TextRecognitionBuildAvailability.Included -> {
                    provider.bestFor?.let {
                        Text(
                            text = stringResource(MR.strings.text_recognition_engine_best_for, it),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    val labels = engineLanguageLabels(components)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        labels.take(SHOWN_LANGUAGE_LABELS).forEach { EnginePill(it) }
                        val more = labels.size - SHOWN_LANGUAGE_LABELS
                        if (more > 0) {
                            EnginePill(stringResource(MR.strings.text_recognition_engine_more_scripts, more))
                        }
                    }
                }
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

/**
 * What an engine reads, one label per script: the language's name for a single-language script, otherwise the
 * script's name and how many languages it covers. Languages no script groups are named on their own.
 */
@Composable
private fun engineLanguageLabels(components: List<KnownTextRecognitionComponent>): List<String> {
    val scripts = components.flatMap { it.scripts }.distinctBy { it.displayName }
    val grouped = scripts.flatMapTo(mutableSetOf()) { it.languages }
    val ungrouped = components.flatMap { it.languages }.distinct().filterNot { it in grouped }
    return scripts.map { script ->
        script.languages.singleOrNull()?.displayName()
            ?: stringResource(
                MR.strings.text_recognition_engine_script_languages,
                script.displayName,
                script.languages.size,
            )
    } + ungrouped.map { it.displayName() }
}

@Composable
private fun EnginePill(label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private const val SHOWN_LANGUAGE_LABELS = 6
private const val EXCLUDED_ALPHA = 0.6f
