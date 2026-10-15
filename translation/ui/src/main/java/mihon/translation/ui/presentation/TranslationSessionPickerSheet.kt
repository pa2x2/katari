package mihon.translation.ui.presentation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import mihon.presentation.kit.theme.KatariTheme
import mihon.translation.ui.picker.engine.TranslationEnginePickerList
import mihon.translation.ui.picker.language.TranslationLanguageDefaultOption
import mihon.translation.ui.picker.language.TranslationLanguageRole
import mihon.translation.ui.picker.language.TranslationLanguageSupportPicker
import mihon.translation.ui.picker.language.translationAutomaticSourceOption
import mihon.translation.ui.picker.language.translationDefaultTargetOption
import mihon.translation.ui.presentation.language.TranslationLanguageScopeCard
import mihon.translation.ui.session.TranslationSessionHostCoordinator
import mihon.translation.ui.session.TranslationSessionLanguageDefault
import mihon.translation.ui.session.TranslationSessionPicker
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TranslationSessionPickerSheet(
    coordinator: TranslationSessionHostCoordinator,
    picker: TranslationSessionPicker,
) {
    val engineStates by coordinator.engineStates.collectAsState()
    TranslationPickerSheet(
        title = stringResource(
            when (picker) {
                TranslationSessionPicker.SourceLanguage -> MR.strings.translation_translate_from
                TranslationSessionPicker.TargetLanguage -> MR.strings.translation_translate_to
                TranslationSessionPicker.Engine -> MR.strings.translation_choose_engine
            },
        ),
        onDismiss = coordinator::dismissPicker,
    ) {
        when (picker) {
            TranslationSessionPicker.SourceLanguage,
            TranslationSessionPicker.TargetLanguage,
            -> TranslationSessionLanguagePicker(coordinator, picker)
            TranslationSessionPicker.Engine -> {
                TranslationEnginePickerList(
                    engines = engineStates,
                    selected = coordinator.activeEngine(),
                    onSelect = coordinator::selectEngine,
                    selectableOnly = true,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.TranslationSessionLanguagePicker(
    coordinator: TranslationSessionHostCoordinator,
    picker: TranslationSessionPicker,
) {
    val engineStates by coordinator.engineStates.collectAsState()
    val languageSupport by coordinator.languageSupport.collectAsState()
    val recentLanguages by coordinator.recentLanguages.collectAsState()
    val engine = coordinator.activeEngine()
    val choices by coordinator.languages.choices.collectAsState()
    val keptTarget = choices.target.takeIf { coordinator.languages.keepsChoices }
    if (picker == TranslationSessionPicker.TargetLanguage && keptTarget != null) {
        TranslationLanguageScopeCard(
            kept = keptTarget,
            defaultTarget = (coordinator.languageDefault(picker) as? TranslationSessionLanguageDefault.Target)
                ?.target
                ?.language,
            onUseDefault = coordinator::selectLanguageDefault,
            onUseForAll = {
                coordinator.makeDefaultTarget(keptTarget)
                coordinator.dismissPicker()
            },
            modifier = Modifier.padding(
                start = KatariTheme.spacing.contentInset,
                top = KatariTheme.spacing.m,
                end = KatariTheme.spacing.contentInset,
            ),
        )
    }
    TranslationLanguageSupportPicker(
        state = languageSupport,
        engine = engine,
        role = when (picker) {
            TranslationSessionPicker.SourceLanguage -> TranslationLanguageRole.Source
            TranslationSessionPicker.TargetLanguage -> TranslationLanguageRole.Target
            TranslationSessionPicker.Engine -> error("$picker has no language role")
        },
        counterpart = coordinator.counterpartLanguage(picker),
        selected = coordinator.selectedLanguage(picker),
        onSelect = coordinator::selectLanguage,
        onRetry = coordinator::retryLanguageSupport,
        engineName = engineStates.firstOrNull { it.engine.id == engine }?.engine?.engineName,
        modifier = Modifier
            .weight(1f, fill = false)
            .padding(top = KatariTheme.spacing.s),
        defaultOption = coordinator.languageDefault(picker)?.toOption(),
        onSelectDefault = coordinator::selectLanguageDefault,
        recentLanguages = recentLanguages,
    )
}

@Composable
private fun TranslationSessionLanguageDefault.toOption(): TranslationLanguageDefaultOption =
    when (this) {
        is TranslationSessionLanguageDefault.Target -> translationDefaultTargetOption(target, selected)
        is TranslationSessionLanguageDefault.AutomaticSource ->
            translationAutomaticSourceOption(detected, declared, selected)
    }
