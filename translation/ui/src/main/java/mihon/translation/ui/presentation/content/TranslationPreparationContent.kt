package mihon.translation.ui.presentation.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import mihon.language.api.tag.LanguageTag
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.ContentButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.translation.api.engine.KnownTranslationEngine
import mihon.translation.api.engine.TranslationEngineBuildAvailability
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.preparation.TranslationEngineChoiceReason
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationTargetChoiceReason
import mihon.translation.api.preparation.TranslationUnavailableReason
import mihon.translation.ui.picker.language.displayName
import mihon.translation.ui.picker.language.translationLanguageOption
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import mihon.translation.ui.presentation.language.TranslationLanguageSuggestionChips
import mihon.translation.ui.session.language.TranslationLanguageSuggestions
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** What a translation still needs before it can run, such as a language, an engine, a download or consent. */
@Composable
internal fun PreparationContent(
    preparation: TranslationPreparation,
    suggestions: TranslationLanguageSuggestions?,
    onRetry: () -> Unit,
    onSelectSource: (LanguageTag) -> Unit,
    onSelectTarget: (LanguageTag) -> Unit,
    onSelectEngine: (TranslationEngineSelection) -> Unit,
    useExternalEnginePicker: Boolean,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    compact: Boolean,
) {
    when (preparation) {
        is TranslationPreparation.ProviderDisclosureRequired -> {
            Text(
                text = preparation.disclosure.title,
                role = TextRole.SectionTitle,
                maxLines = if (compact) 1 else Int.MAX_VALUE,
                overflow = if (compact) TextOverflow.Ellipsis else TextOverflow.Clip,
            )
            SessionMessage(preparation.disclosure.message, compact)
            SessionActionButton(
                label = preparation.disclosure.confirmationLabel,
                onClick = {
                    onExternalAction(
                        TranslationSessionExternalAction.ConfirmProviderDisclosure(
                            preparation.engine,
                            preparation.disclosure,
                        ),
                    )
                },
            )
            DocumentationAction(
                preparation.disclosure.documentationUrl ?: preparation.presentation.documentationUrl,
                onExternalAction,
                compact,
            )
        }
        is TranslationPreparation.ModelDownloadRequired -> {
            SessionMessage(stringResource(MR.strings.translation_models_required), compact)
            if (!compact) {
                preparation.models.forEach { model ->
                    Text("• ${model.displayName}", role = TextRole.Body)
                }
            }
            SessionActionButton(
                label = stringResource(MR.strings.action_download),
                onClick = {
                    onExternalAction(
                        TranslationSessionExternalAction.DownloadModels(
                            preparation.engine,
                            preparation.models,
                        ),
                    )
                },
            )
            DocumentationAction(preparation.presentation.documentationUrl, onExternalAction, compact)
        }
        is TranslationPreparation.SystemSetupRequired -> {
            SessionMessage(preparation.reason.message(), compact)
            SessionActionButton(
                label = stringResource(MR.strings.action_settings),
                onClick = {
                    onExternalAction(TranslationSessionExternalAction.OpenSetup(preparation.engine))
                },
            )
            DocumentationAction(preparation.presentation.documentationUrl, onExternalAction, compact)
        }
        is TranslationPreparation.SetupInProgress -> {
            SessionMessage(stringResource(MR.strings.translation_setup_in_progress), compact)
            val progress = preparation.progress
            val total = progress?.total
            if (progress != null && total != null && total > 0) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = { progress.completed.toFloat() / total.toFloat() },
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        is TranslationPreparation.SourceUndetermined -> {
            SessionMessage(stringResource(MR.strings.translation_source_undetermined), compact)
            if (suggestions != null) {
                TranslationLanguageSuggestionChips(
                    languages = suggestions.languages,
                    label = { it.displayName() },
                    moreLabel = stringResource(MR.strings.translation_language_other),
                    onSelect = onSelectSource,
                    onMore = { onExternalAction(TranslationSessionExternalAction.ChooseSourceLanguage) },
                )
                return
            }
            if (!compact) {
                preparation.suggestedLanguages.forEach { language ->
                    Button(
                        label = language.displayName(),
                        onClick = { onSelectSource(language) },
                        modifier = Modifier.fillMaxWidth(),
                        style = ButtonStyle.Text,
                    )
                }
            }
            SessionActionButton(
                label = stringResource(MR.strings.translation_choose_source_language),
                onClick = {
                    onExternalAction(TranslationSessionExternalAction.ChooseSourceLanguage)
                },
            )
        }
        is TranslationPreparation.TargetLanguageRequired -> {
            if (suggestions != null) {
                TargetSuggestions(
                    message = when (preparation.reason) {
                        TranslationTargetChoiceReason.NoDefaultTarget ->
                            stringResource(MR.strings.translation_target_required_suggestions)
                        TranslationTargetChoiceReason.SourceEqualsTarget ->
                            preparation.sourceLanguage
                                ?.let {
                                    stringResource(
                                        MR.strings.translation_source_equals_target_suggestions,
                                        it.displayName(),
                                    )
                                }
                                ?: stringResource(MR.strings.translation_source_equals_target)
                    },
                    suggestions = suggestions,
                    onSelectTarget = onSelectTarget,
                    onExternalAction = onExternalAction,
                    compact = compact,
                )
                return
            }
            SessionMessage(
                text = when (preparation.reason) {
                    TranslationTargetChoiceReason.NoDefaultTarget ->
                        stringResource(MR.strings.translation_target_required)
                    TranslationTargetChoiceReason.SourceEqualsTarget ->
                        stringResource(MR.strings.translation_source_equals_target)
                },
                compact = compact,
            )
            SessionActionButton(
                label = stringResource(MR.strings.translation_choose_target_language),
                onClick = {
                    onExternalAction(TranslationSessionExternalAction.ChooseTargetLanguage)
                },
            )
        }
        is TranslationPreparation.EngineChoiceRequired -> {
            SessionMessage(
                text = stringResource(
                    when (preparation.reason) {
                        TranslationEngineChoiceReason.NoEngineConfigured ->
                            MR.strings.translation_engine_not_configured
                        is TranslationEngineChoiceReason.SelectedEngineUnavailable ->
                            MR.strings.translation_selected_engine_unavailable
                    },
                ),
                compact = compact,
            )
            if (compact || useExternalEnginePicker) {
                SessionActionButton(
                    label = stringResource(MR.strings.translation_choose_engine),
                    onClick = {
                        onExternalAction(TranslationSessionExternalAction.ChooseEngine)
                    },
                )
            } else {
                preparation.engines.forEach { engine ->
                    EngineChoice(engine, onSelectEngine)
                }
            }
        }
        is TranslationPreparation.Unavailable -> {
            val unsupportedPair = preparation.reason as? TranslationUnavailableReason.UnsupportedLanguagePair
            if (unsupportedPair != null && suggestions != null) {
                TargetSuggestions(
                    message = unsupportedPair.suggestionsMessage(suggestions.engineName),
                    suggestions = suggestions,
                    onSelectTarget = onSelectTarget,
                    onExternalAction = onExternalAction,
                    compact = compact,
                )
                ChangeEngineAction(onExternalAction)
                return
            }
            SessionMessage(preparation.reason.message(), compact)
            if (unsupportedPair != null) {
                SessionActionButton(
                    label = stringResource(MR.strings.translation_choose_target_language),
                    onClick = {
                        onExternalAction(TranslationSessionExternalAction.ChooseTargetLanguage)
                    },
                )
                ChangeEngineAction(onExternalAction)
            } else {
                SessionActionButton(
                    label = stringResource(MR.strings.action_retry),
                    onClick = onRetry,
                )
            }
        }
        is TranslationPreparation.Rejected -> {
            SessionMessage(preparation.reason.message(), compact)
        }
        is TranslationPreparation.Ready -> error("Ready preparation must use TranslationSessionState.Ready")
    }
}

@Composable
private fun TargetSuggestions(
    message: String,
    suggestions: TranslationLanguageSuggestions,
    onSelectTarget: (LanguageTag) -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    compact: Boolean,
) {
    SessionMessage(message, compact)
    TranslationLanguageSuggestionChips(
        languages = suggestions.languages,
        label = { translationLanguageOption(it).nativeName },
        moreLabel = stringResource(MR.strings.translation_language_more),
        onSelect = onSelectTarget,
        onMore = { onExternalAction(TranslationSessionExternalAction.ChooseTargetLanguage) },
    )
}

@Composable
private fun ChangeEngineAction(onExternalAction: (TranslationSessionExternalAction) -> Unit) {
    Button(
        label = stringResource(MR.strings.translation_change_engine),
        onClick = { onExternalAction(TranslationSessionExternalAction.ChooseEngine) },
        style = ButtonStyle.Text,
    )
}

@Composable
private fun TranslationUnavailableReason.UnsupportedLanguagePair.suggestionsMessage(engineName: String?): String =
    if (engineName != null) {
        stringResource(
            MR.strings.translation_unsupported_pair_engine_suggestions,
            engineName,
            source.displayName(),
            target.displayName(),
        )
    } else {
        stringResource(MR.strings.translation_unsupported_pair_suggestions, source.displayName(), target.displayName())
    }

// A two-line text button: the engine's name over who provides it, or why this build leaves it out.
@Composable
private fun EngineChoice(
    engine: KnownTranslationEngine,
    onSelectEngine: (TranslationEngineSelection) -> Unit,
) {
    val availability = engine.buildAvailability
    val included = availability is TranslationEngineBuildAvailability.Included
    val tone = if (included) ContentTone.Primary else ContentTone.Disabled
    ContentButton(
        onClick = { onSelectEngine(TranslationEngineSelection.Explicit(engine.id)) },
        modifier = Modifier.fillMaxWidth(),
        enabled = included,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
        ) {
            Text(engine.engineName, role = TextRole.Label, tone = tone)
            Text(
                text = when (availability) {
                    TranslationEngineBuildAvailability.Included -> engine.providerName
                    is TranslationEngineBuildAvailability.NotIncluded -> availability.reason
                },
                role = TextRole.Supporting,
                tone = tone,
            )
        }
    }
}
