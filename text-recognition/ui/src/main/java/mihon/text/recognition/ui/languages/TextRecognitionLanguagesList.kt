package mihon.text.recognition.ui.languages

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Add
import mihon.icons.materialsymbols.rounded.Delete
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.SectionHeader
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.provider.KnownTextRecognitionProvider
import mihon.text.recognition.ui.language.displayName
import mihon.text.recognition.ui.language.shortDisplayNames
import tachiyomi.i18n.*
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
    contentPadding: PaddingValues = PaddingValues(vertical = KatariTheme.spacing.s),
) {
    fun providerName(group: TextRecognitionLanguageGroup): String =
        providers.firstOrNull { it.id == group.preset.provider }?.name ?: group.preset.provider.value

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "choices-heading") {
            SectionHeader(stringResource(MR.strings.text_recognition_languages_your_choices))
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
                            role = TextRole.Supporting,
                            tone = ContentTone.Error,
                        )
                    } else {
                        TextRecognitionLanguageModelStatus(models(pipeline, it.language), modelStates)
                    }
                },
                onClick = { onChooseLanguage(it.language) },
                trailing = {
                    IconButton(
                        icon = MaterialSymbols.Rounded.Delete,
                        contentDescription = stringResource(MR.strings.action_remove),
                        onClick = { onRemoveChoice(it) },
                    )
                },
            )
        }
        item(key = "add-choice") {
            SettingsRow(
                title = stringResource(MR.strings.text_recognition_languages_add),
                leading = { Icon(MaterialSymbols.Rounded.Add, contentDescription = null, tone = ContentTone.Primary) },
                onClick = onAddChoice,
            )
        }
        overview.engineGroups.firstOrNull()?.let { first ->
            item(key = "engine-heading") { SectionHeader(providerName(first)) }
            groupRows(overview.engineGroups, models, modelStates, onChooseLanguage, onChooseInGroup)
        }
        overview.otherEngineGroups.groupBy(::providerName).forEach { (provider, groups) ->
            item(key = "other-heading-$provider") {
                SectionHeader(stringResource(MR.strings.text_recognition_languages_read_by, provider))
            }
            groupRows(groups, models, modelStates, onChooseLanguage, onChooseInGroup)
        }
        if (overview.choiceRequired.isNotEmpty()) {
            item(key = "choice-required-heading") {
                SectionHeader(stringResource(MR.strings.text_recognition_languages_choice_required))
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
            title = group.languages.shortDisplayNames(),
            pipelineLabel = group.preset.displayName,
            status = {
                TextRecognitionLanguageModelStatus(models(group.preset.pipeline, group.languages.first()), modelStates)
            },
            onClick = { if (single != null) onChooseLanguage(single) else onChooseInGroup(group.languages) },
        )
    }
}

@Composable
private fun LanguageRow(
    title: String,
    pipelineLabel: String,
    status: @Composable () -> Unit,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    SettingsRow(
        title = title,
        subtitle = pipelineLabel,
        trailing = trailing,
        onClick = onClick,
        below = status,
    )
}
