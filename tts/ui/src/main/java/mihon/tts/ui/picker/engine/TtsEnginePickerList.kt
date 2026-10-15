package mihon.tts.ui.picker.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import mihon.presentation.kit.theme.KatariTheme
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.engine.TtsEngineInspection

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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .wrapContentWidth()
            .widthIn(max = KatariTheme.sizes.readableWidth),
        contentPadding = PaddingValues(KatariTheme.spacing.contentInset),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
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
    detailsModel?.let { model ->
        TtsEngineDetailsSheet(
            model = model,
            onDismissRequest = { detailsEngineId = null },
            onOpenUrl = onOpenDocumentation,
        )
    }
}
