package mihon.tts.ui.picker.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.engine.TtsEngineInspection
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TtsEnginePickerList(
    inspection: TtsEngineInspection,
    setupEngines: Set<TtsEngineId>,
    onSelect: (TtsEngineId) -> Unit,
    onOpenSetup: (TtsEngineId) -> Unit,
    onOpenDocumentation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedEngine = inspection.selectedEngine
    val cards = remember(inspection.engines, selectedEngine, setupEngines) {
        inspection.engines.map { state ->
            projectTtsEngineCard(state, selectedEngine, state.engine.id in setupEngines)
        }
    }
    var detailsEngineId by remember { mutableStateOf<TtsEngineId?>(null) }
    val detailsModel = cards.firstOrNull { it.state.engine.id == detailsEngineId }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (
                selectedEngine != null &&
                inspection.engines.none { it.engine.id == selectedEngine }
            ) {
                item(key = "missing-selection") {
                    TtsMissingEngineCard(selectedEngine)
                }
            }
            items(cards, key = { it.state.engine.id.value }) { model ->
                TtsEngineCard(
                    model = model,
                    onSelect = onSelect,
                    onOpenSetup = onOpenSetup,
                    onOpenDetails = { detailsEngineId = model.state.engine.id },
                )
            }
        }
    }
    detailsModel?.let { model ->
        TtsEngineDetailsSheet(
            model = model,
            onDismissRequest = { detailsEngineId = null },
            onOpenUrl = onOpenDocumentation,
        )
    }
}

@Composable
private fun TtsMissingEngineCard(selected: TtsEngineId) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(MR.strings.tts_settings_engine_unavailable),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(MR.strings.tts_settings_engine_missing_id, selected.value),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(MR.strings.tts_settings_engine_missing_explanation),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
