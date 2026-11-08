package mihon.translation.ui.picker.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.language.api.tag.LanguageTag
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.ui.session.TranslationLanguageSupportState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TranslationLanguageSupportPicker(
    state: TranslationLanguageSupportState,
    engine: TranslationEngineId?,
    role: TranslationLanguageRole,
    counterpart: LanguageTag?,
    selected: LanguageTag?,
    onSelect: (LanguageTag) -> Unit,
    onRetry: () -> Unit,
    engineName: String?,
    modifier: Modifier = Modifier,
    defaultOption: TranslationLanguageDefaultOption? = null,
    onSelectDefault: () -> Unit = {},
    recentLanguages: List<LanguageTag> = emptyList(),
) {
    when (state) {
        is TranslationLanguageSupportState.Available -> {
            if (state.engine != engine) {
                TranslationLanguageSupportLoading(modifier)
                return
            }
            val options = remember(state.support, role, counterpart) {
                translationLanguageOptions(state.support, role, counterpart)
            }
            val recentOptions = remember(options, recentLanguages) {
                translationRecentLanguageOptions(recentLanguages, options)
            }
            val unpairable = remember(state.support, role, counterpart, engineName) {
                counterpart?.let { source ->
                    TranslationUnpairableLanguages(
                        source = source,
                        engineName = engineName ?: state.engine.value,
                        options = translationLanguageOptions(state.support.unpairableLanguages(role, source)),
                    )
                }
            }
            TranslationLanguagePickerList(
                options = options,
                selected = selected,
                onSelect = onSelect,
                modifier = modifier,
                defaultOption = defaultOption,
                onSelectDefault = onSelectDefault,
                shortcuts = recentOptions,
                unpairable = unpairable,
            )
        }
        TranslationLanguageSupportState.Idle -> {
            if (engine == null) {
                TranslationLanguageSupportUnavailable(
                    reason = stringResource(MR.strings.translation_engine_not_configured),
                    onRetry = null,
                    modifier = modifier,
                )
            } else {
                TranslationLanguageSupportLoading(modifier)
            }
        }
        is TranslationLanguageSupportState.Loading -> TranslationLanguageSupportLoading(modifier)
        is TranslationLanguageSupportState.Unavailable -> {
            TranslationLanguageSupportUnavailable(
                reason = state.reason,
                onRetry = onRetry,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun TranslationLanguageSupportLoading(modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(KatariTheme.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        LoadingIndicator()
    }
}

@Composable
private fun TranslationLanguageSupportUnavailable(
    reason: String?,
    onRetry: (() -> Unit)?,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(KatariTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(reason ?: stringResource(MR.strings.translation_languages_unavailable), role = TextRole.Body)
        onRetry?.let {
            Button(label = stringResource(MR.strings.action_retry), onClick = it)
        }
    }
}
