package eu.kanade.presentation.more.settings.screen.tts.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AppBar
import mihon.tts.ui.settings.TtsVoiceCatalogState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen

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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onAction) {
                Text(action)
            }
        }
    }
}
