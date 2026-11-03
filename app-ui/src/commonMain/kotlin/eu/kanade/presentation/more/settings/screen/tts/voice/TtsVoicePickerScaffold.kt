package eu.kanade.presentation.more.settings.screen.tts.voice

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.theme.KatariTheme
import mihon.tts.ui.settings.TtsVoiceCatalogState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TtsVoicePickerScaffold(
    title: String,
    catalog: TtsVoiceCatalogState,
    onBack: () -> Unit,
    onChooseEngine: () -> Unit,
    onInstallVoiceData: () -> Unit,
    onRetry: () -> Unit,
    content: @Composable (TtsVoiceCatalogState.Available, PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            AppBar(
                title = title,
                navigateUp = onBack,
                scrollBehavior = it,
            )
        },
    ) { contentPadding ->
        when (catalog) {
            TtsVoiceCatalogState.NoEngine -> TtsVoiceCatalogMessage(
                message = stringResource(MR.strings.tts_settings_engine_required),
                action = stringResource(MR.strings.tts_settings_choose_engine),
                onAction = onChooseEngine,
                contentPadding = contentPadding,
            )
            is TtsVoiceCatalogState.Loading -> LoadingScreen(Modifier.padding(contentPadding))
            is TtsVoiceCatalogState.Available -> content(catalog, contentPadding)
            is TtsVoiceCatalogState.VoiceDataRequired -> TtsVoiceCatalogMessage(
                message = catalog.reason ?: stringResource(MR.strings.tts_settings_voice_data_required),
                action = stringResource(MR.strings.tts_settings_engine_manage),
                onAction = onInstallVoiceData,
                contentPadding = contentPadding,
            )
            is TtsVoiceCatalogState.Unavailable -> TtsVoiceCatalogMessage(
                message = catalog.reason ?: stringResource(MR.strings.tts_settings_voices_unavailable),
                action = stringResource(MR.strings.action_retry),
                onAction = onRetry,
                contentPadding = contentPadding,
            )
            is TtsVoiceCatalogState.Failed -> TtsVoiceCatalogMessage(
                message = catalog.reason,
                action = stringResource(MR.strings.action_retry),
                onAction = onRetry,
                contentPadding = contentPadding,
            )
        }
    }
}

@Composable
private fun TtsVoiceCatalogMessage(
    message: String,
    action: String,
    onAction: () -> Unit,
    contentPadding: PaddingValues,
) {
    EmptyScreen(message = message, modifier = Modifier.padding(contentPadding)) {
        Button(label = action, onClick = onAction, modifier = Modifier.padding(top = KatariTheme.spacing.xl))
    }
}
