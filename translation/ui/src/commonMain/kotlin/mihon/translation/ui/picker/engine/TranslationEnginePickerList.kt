package mihon.translation.ui.picker.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.theme.KatariTheme
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineState

@Composable
fun TranslationEnginePickerList(
    engines: List<TranslationEngineState>,
    selected: TranslationEngineId?,
    onSelect: (TranslationEngineId) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSetup: (TranslationEngineId) -> Unit = {},
    onOpenDocumentation: (String) -> Unit = {},
    selectableOnly: Boolean = false,
    showManagementActions: Boolean = false,
    showMissingSelectionNotice: Boolean = false,
    footerLabel: String? = null,
    onFooterClick: (() -> Unit)? = null,
) {
    val picker = remember(engines, selected) {
        projectTranslationEnginePicker(engines, selected)
    }
    val visibleCards = remember(picker.cards, selectableOnly) {
        picker.cards.filter { !selectableOnly || it.selectable }
    }
    var detailsEngineId by remember { mutableStateOf<TranslationEngineId?>(null) }
    val detailsModel = picker.cards.firstOrNull { it.state.engine.id == detailsEngineId }

    RowList(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(KatariTheme.spacing.contentInset),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
    ) {
        if (
            showMissingSelectionNotice &&
            selected != null &&
            isTranslationEngineSelectionMissing(engines, selected)
        ) {
            item(key = "missing-selection") {
                TranslationMissingEngineCard(selected)
            }
        }
        items(visibleCards, key = { it.state.engine.id.value }) { model ->
            TranslationEngineCard(
                model = model,
                onSelect = onSelect,
                onOpenSetup = onOpenSetup,
                onOpenDetails = { detailsEngineId = model.state.engine.id },
                showManagementActions = showManagementActions,
            )
        }
        if (footerLabel != null && onFooterClick != null) {
            item(key = "footer") {
                Button(
                    label = footerLabel,
                    onClick = onFooterClick,
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Text,
                )
            }
        }
    }
    detailsModel?.let { model ->
        TranslationEngineDetailsSheet(
            model = model,
            onDismissRequest = { detailsEngineId = null },
            onOpenUrl = onOpenDocumentation,
        )
    }
}
