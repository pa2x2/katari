package mihon.text.recognition.ui.languages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.model.artifacts.ui.state.formatModelArtifactSize
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.provider.KnownTextRecognitionProvider
import mihon.text.recognition.ui.language.displayName
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsItemsPaddings
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The profile's language choices, then how the engine reads every other language: one row per preset, with the state
 * of the models it needs. Tapping a row changes how its language, or one of its languages, is read.
 */
@Composable
fun TextRecognitionLanguagesList(
    overview: TextRecognitionLanguageOverview,
    providers: List<KnownTextRecognitionProvider>,
    selectionLabel: @Composable (TextRecognitionPipelineSelection) -> String,
    models: (TextRecognitionPipeline, LanguageTag) -> List<ModelArtifactDescriptor>,
    modelStates: Map<ModelArtifactDescriptor, ModelArtifactState>,
    onChooseLanguage: (LanguageTag) -> Unit,
    onChooseInGroup: (List<LanguageTag>) -> Unit,
    onRemoveChoice: (TextRecognitionLanguageChoice) -> Unit,
    onAddChoice: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
) {
    fun providerName(group: TextRecognitionLanguageGroup): String =
        providers.firstOrNull { it.id == group.preset.provider }?.name ?: group.preset.provider.value

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "choices-heading") {
            HeadingItem(stringResource(MR.strings.text_recognition_languages_your_choices))
        }
        items(overview.choices.sortedBy { it.language.displayName() }, key = { "choice-${it.language.value}" }) {
            val pipeline = it.pipeline
            LanguageRow(
                title = it.language.displayName(),
                pipelineLabel = selectionLabel(it.selection),
                status = {
                    if (pipeline == null) {
                        Text(
                            text = stringResource(MR.strings.text_recognition_languages_unavailable),
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        ModelStatus(models(pipeline, it.language), modelStates)
                    }
                },
                onClick = { onChooseLanguage(it.language) },
                trailing = {
                    IconButton(onClick = { onRemoveChoice(it) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(MR.strings.action_remove))
                    }
                },
            )
        }
        item(key = "add-choice") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddChoice)
                    .padding(horizontal = SettingsItemsPaddings.Horizontal, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(MR.strings.text_recognition_languages_add),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        overview.engineGroups.firstOrNull()?.let { first ->
            item(key = "engine-heading") { HeadingItem(providerName(first)) }
            groupRows(overview.engineGroups, models, modelStates, onChooseLanguage, onChooseInGroup)
        }
        overview.otherEngineGroups.groupBy(::providerName).forEach { (provider, groups) ->
            item(key = "other-heading-$provider") {
                HeadingItem(stringResource(MR.strings.text_recognition_languages_read_by, provider))
            }
            groupRows(groups, models, modelStates, onChooseLanguage, onChooseInGroup)
        }
        if (overview.choiceRequired.isNotEmpty()) {
            item(key = "choice-required-heading") {
                HeadingItem(stringResource(MR.strings.text_recognition_languages_choice_required))
            }
            items(overview.choiceRequired, key = { "required-${it.value}" }) { language ->
                LanguageRow(
                    title = language.displayName(),
                    pipelineLabel = stringResource(MR.strings.text_recognition_languages_choose_pipeline),
                    status = {},
                    onClick = { onChooseLanguage(language) },
                )
            }
        }
    }
}

private fun LazyListScope.groupRows(
    groups: List<TextRecognitionLanguageGroup>,
    models: (TextRecognitionPipeline, LanguageTag) -> List<ModelArtifactDescriptor>,
    modelStates: Map<ModelArtifactDescriptor, ModelArtifactState>,
    onChooseLanguage: (LanguageTag) -> Unit,
    onChooseInGroup: (List<LanguageTag>) -> Unit,
) {
    items(groups, key = { "group-${it.preset.id.value}" }) { group ->
        val single = group.languages.singleOrNull()
        LanguageRow(
            title = languageNames(group.languages),
            pipelineLabel = group.preset.displayName,
            status = { ModelStatus(models(group.preset.pipeline, group.languages.first()), modelStates) },
            onClick = { if (single != null) onChooseLanguage(single) else onChooseInGroup(group.languages) },
        )
    }
}

/** Up to three language names, then how many more the group has. */
@Composable
private fun languageNames(languages: List<LanguageTag>): String {
    val shown = languages.take(NAMED_LANGUAGES).joinToString { it.displayName() }
    val more = languages.size - NAMED_LANGUAGES
    return if (more > 0) stringResource(MR.strings.text_recognition_languages_more, shown, more) else shown
}

@Composable
private fun LanguageRow(
    title: String,
    pipelineLabel: String,
    status: @Composable () -> Unit,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = SettingsItemsPaddings.Horizontal, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = pipelineLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ProvideTextStyle(MaterialTheme.typography.bodySmall) { status() }
        }
        trailing()
    }
}

/** Whether the models a row's pipeline needs are downloaded, downloading, or still missing and how large they are. */
@Composable
private fun ModelStatus(
    models: List<ModelArtifactDescriptor>,
    states: Map<ModelArtifactDescriptor, ModelArtifactState>,
) {
    if (models.isEmpty()) return
    val modelStates = models.map { it to (states[it] ?: ModelArtifactState.NotInstalled) }
    val missing = modelStates.filter { (_, state) ->
        state !is ModelArtifactState.Installed && state !is ModelArtifactState.Downloading
    }
    when {
        modelStates.any { (_, state) -> state is ModelArtifactState.Downloading } -> Text(
            text = stringResource(MR.strings.text_recognition_languages_models_downloading),
            color = MaterialTheme.colorScheme.primary,
        )
        missing.isNotEmpty() -> Text(
            text = stringResource(
                MR.strings.text_recognition_languages_models_needed,
                formatModelArtifactSize(missing.sumOf { (model, _) -> model.sizeBytes }),
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        else -> Text(
            text = stringResource(MR.strings.text_recognition_languages_models_downloaded),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val NAMED_LANGUAGES = 3
