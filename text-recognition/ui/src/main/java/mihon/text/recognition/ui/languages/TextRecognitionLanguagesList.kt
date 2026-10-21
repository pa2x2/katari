package mihon.text.recognition.ui.languages

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Add
import mihon.icons.materialsymbols.rounded.Delete
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.list.rowGroupItems
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

    val choices = overview.choices.sortedBy { it.language.displayName() }
    // The choices and the row that adds one share a card.
    val choiceRowCount = choices.size + 1
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "choices-heading") {
            RowGroupHeader(stringResource(MR.strings.text_recognition_languages_your_choices))
        }
        itemsIndexed(choices, key = { _, choice -> "choice-${choice.language.value}" }) { index, choice ->
            val pipeline = choice.pipeline
            RowGroupItem(position = RowGroupPosition.of(index, choiceRowCount), modifier = Modifier.animateItem()) {
                LanguageRow(
                    title = choice.language.displayName(),
                    pipelineLabel = selectionLabel(choice.selection),
                    status = {
                        if (pipeline == null) {
                            Text(
                                text = stringResource(MR.strings.text_recognition_languages_unavailable),
                                role = TextRole.Supporting,
                                tone = ContentTone.Error,
                            )
                        } else {
                            TextRecognitionLanguageModelStatus(models(pipeline, choice.language), modelStates)
                        }
                    },
                    onClick = { onChooseLanguage(choice.language) },
                    trailing = {
                        IconButton(
                            icon = MaterialSymbols.Rounded.Delete,
                            contentDescription = stringResource(MR.strings.action_remove),
                            onClick = { onRemoveChoice(choice) },
                        )
                    },
                )
            }
        }
        item(key = "add-choice") {
            RowGroupItem(
                position = RowGroupPosition.of(choiceRowCount - 1, choiceRowCount),
                modifier = Modifier.animateItem(),
            ) {
                SettingsRow(
                    title = stringResource(MR.strings.text_recognition_languages_add),
                    icon = MaterialSymbols.Rounded.Add,
                    onClick = onAddChoice,
                )
            }
        }
        overview.engineGroups.firstOrNull()?.let { first ->
            groupSection(
                key = "engine",
                title = { providerName(first) },
                groups = overview.engineGroups,
                models = models,
                modelStates = modelStates,
                onChooseLanguage = onChooseLanguage,
                onChooseInGroup = onChooseInGroup,
            )
        }
        overview.otherEngineGroups.groupBy(::providerName).forEach { (provider, groups) ->
            groupSection(
                key = "other-$provider",
                title = { stringResource(MR.strings.text_recognition_languages_read_by, provider) },
                groups = groups,
                models = models,
                modelStates = modelStates,
                onChooseLanguage = onChooseLanguage,
                onChooseInGroup = onChooseInGroup,
            )
        }
        if (overview.choiceRequired.isNotEmpty()) {
            item(key = "choice-required-gap") { RowGroupGap() }
            item(key = "choice-required-heading") {
                RowGroupHeader(stringResource(MR.strings.text_recognition_languages_choice_required))
            }
            rowGroupItems(overview.choiceRequired, key = { "required-${it.value}" }) { language ->
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

/** A heading and the card of the presets [groups] read their languages with, set apart from the section above. */
private fun LazyListScope.groupSection(
    key: String,
    title: @Composable () -> String,
    groups: List<TextRecognitionLanguageGroup>,
    models: (TextRecognitionPipeline, LanguageTag) -> List<ModelArtifactDescriptor>,
    modelStates: Map<ModelArtifactDescriptor, ModelArtifactState>,
    onChooseLanguage: (LanguageTag) -> Unit,
    onChooseInGroup: (List<LanguageTag>) -> Unit,
) {
    item(key = "$key-gap") { RowGroupGap() }
    item(key = "$key-heading") { RowGroupHeader(title()) }
    rowGroupItems(groups, key = { "group-${it.preset.id.value}" }) { group ->
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
