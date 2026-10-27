package mihon.translation.ui.picker.engine

import android.R
import android.content.res.Configuration
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
import androidx.compose.ui.tooling.preview.Preview
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.theme.KatariPreviewTheme
import mihon.presentation.kit.theme.KatariTheme
import mihon.translation.api.engine.KnownTranslationEngine
import mihon.translation.api.engine.TranslationEngineAction
import mihon.translation.api.engine.TranslationEngineArtwork
import mihon.translation.api.engine.TranslationEngineBuildAvailability
import mihon.translation.api.engine.TranslationEngineDetails
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineState
import mihon.translation.api.engine.TranslationEngineStatus
import mihon.translation.api.engine.TranslationProviderId

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

@Preview(name = "Full picker")
@Preview(name = "Full picker dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TranslationEnginePickerPreview() {
    KatariPreviewTheme {
        Surface {
            TranslationEnginePickerList(
                engines = previewEngineStates(),
                selected = PREVIEW_READY_ENGINE.id,
                onSelect = {},
                showManagementActions = true,
                showMissingSelectionNotice = true,
            )
        }
    }
}

@Preview(name = "Compact picker", widthDp = 360, heightDp = 640)
@Preview(name = "Large font", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun TranslationEngineSessionPickerPreview() {
    KatariPreviewTheme {
        Surface {
            TranslationEnginePickerList(
                engines = previewEngineStates(),
                selected = PREVIEW_UNAVAILABLE_ENGINE.id,
                onSelect = {},
                selectableOnly = true,
            )
        }
    }
}

@Preview(name = "Missing stored engine", widthDp = 400, heightDp = 720)
@Composable
private fun TranslationEngineMissingSelectionPreview() {
    KatariPreviewTheme {
        Surface {
            TranslationEnginePickerList(
                engines = previewEngineStates(),
                selected = TranslationEngineId("removed-engine"),
                onSelect = {},
                showManagementActions = true,
                showMissingSelectionNotice = true,
            )
        }
    }
}

private fun previewEngineStates() = listOf(
    TranslationEngineState(
        engine = PREVIEW_READY_ENGINE,
        presentation = null,
        status = TranslationEngineStatus.Ready,
        action = TranslationEngineAction.Setup,
    ),
    TranslationEngineState(
        engine = PREVIEW_UNAVAILABLE_ENGINE,
        presentation = null,
        status = TranslationEngineStatus.NotInstalled,
        action = TranslationEngineAction.Install,
    ),
    TranslationEngineState(
        engine = PREVIEW_CONFIGURE_ENGINE,
        presentation = null,
        status = TranslationEngineStatus.ConfigurationRequired("Enter and test a server endpoint."),
        action = TranslationEngineAction.Configure,
    ),
)

private val PREVIEW_ARTWORK = TranslationEngineArtwork.Bundled(R.drawable.ic_dialog_info)
private val PREVIEW_DETAILS = TranslationEngineDetails(
    description = "Translation through a provider-owned engine.",
    processingLocation = "The configured translation provider.",
    privacyDescription = "The provider privacy policy applies.",
)
private val PREVIEW_READY_ENGINE = previewEngine("android-system", "Android System Translation")
private val PREVIEW_UNAVAILABLE_ENGINE = previewEngine("offline-translator", "Offline Translator")
private val PREVIEW_CONFIGURE_ENGINE = previewEngine("libretranslate-server", "LibreTranslate Server")

private fun previewEngine(id: String, name: String) = KnownTranslationEngine(
    id = TranslationEngineId(id),
    providerId = TranslationProviderId("preview-$id"),
    providerName = "Preview provider",
    engineName = name,
    buildAvailability = TranslationEngineBuildAvailability.Included,
    artwork = PREVIEW_ARTWORK,
    details = PREVIEW_DETAILS,
)
