package mihon.translation.ui.picker.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.ui.session.TranslationLanguageSupportState
import tachiyomi.i18n.MR
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
    modifier: Modifier = Modifier,
    defaultOptionLabel: String? = null,
    defaultOptionSupporting: String? = null,
    defaultSelected: Boolean = false,
    onSelectDefault: (() -> Unit)? = null,
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
            TranslationLanguagePickerList(
                options = options,
                selected = selected,
                onSelect = onSelect,
                modifier = modifier,
                defaultOptionLabel = defaultOptionLabel,
                defaultOptionSupporting = defaultOptionSupporting,
                defaultSelected = defaultSelected,
                onSelectDefault = onSelectDefault,
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
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
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
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(reason ?: stringResource(MR.strings.translation_languages_unavailable))
        onRetry?.let {
            Button(onClick = it) {
                Text(stringResource(MR.strings.action_retry))
            }
        }
    }
}
