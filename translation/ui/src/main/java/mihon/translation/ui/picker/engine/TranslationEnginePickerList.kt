package mihon.translation.ui.picker

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.TranslationEngineAction
import mihon.translation.api.TranslationEngineArtwork
import mihon.translation.api.TranslationEngineBuildAvailability
import mihon.translation.api.TranslationEngineDetails
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationEngineState
import mihon.translation.api.TranslationEngineStatus
import mihon.translation.api.TranslationProviderId
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TranslationEnginePickerList(
    engines: List<TranslationEngineState>,
    selected: TranslationEngineId?,
    density: TranslationEnginePickerDensity,
    onSelect: (TranslationEngineId) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSetup: (TranslationEngineId) -> Unit = {},
    onOpenDocumentation: (String) -> Unit = {},
    showMissingSelectionNotice: Boolean = false,
) {
    val picker = remember(engines, selected, density) {
        projectTranslationEnginePicker(engines, selected, density)
    }
    var detailsEngineId by remember { mutableStateOf<TranslationEngineId?>(null) }
    val detailsModel = picker.cards.firstOrNull { it.state.engine.id == detailsEngineId }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (density == TranslationEnginePickerDensity.Full) {
                item(key = "policy") {
                    TranslationEnginePolicyBanner()
                }
            }
            if (
                showMissingSelectionNotice &&
                selected != null &&
                isTranslationEngineSelectionMissing(engines, selected)
            ) {
                item(key = "missing-selection") {
                    MissingTranslationEngineCard(selected)
                }
            }
            items(picker.cards, key = { it.state.engine.id.value }) { model ->
                TranslationEngineCard(
                    model = model,
                    density = picker.density,
                    onSelect = onSelect,
                    onOpenSetup = onOpenSetup,
                    onOpenDetails = { detailsEngineId = model.state.engine.id },
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

@Composable
private fun TranslationEnginePolicyBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(MR.strings.translation_settings_explicit_engine_notice),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun MissingTranslationEngineCard(selected: TranslationEngineId) {
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
                text = stringResource(MR.strings.translation_engine_missing_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(MR.strings.translation_engine_missing_id, selected.value),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(MR.strings.translation_engine_missing_explanation),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(name = "Full picker")
@Preview(name = "Full picker dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TranslationEnginePickerPreview() {
    TachiyomiPreviewTheme {
        Surface {
            TranslationEnginePickerList(
                engines = previewEngineStates(),
                selected = PREVIEW_READY_ENGINE.id,
                density = TranslationEnginePickerDensity.Full,
                onSelect = {},
                showMissingSelectionNotice = true,
            )
        }
    }
}

@Preview(name = "Compact picker", widthDp = 360, heightDp = 640)
@Preview(name = "Large font", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun TranslationEngineCompactPickerPreview() {
    TachiyomiPreviewTheme {
        Surface {
            TranslationEnginePickerList(
                engines = previewEngineStates(),
                selected = PREVIEW_UNAVAILABLE_ENGINE.id,
                density = TranslationEnginePickerDensity.Compact,
                onSelect = {},
            )
        }
    }
}

@Preview(name = "Missing stored engine", widthDp = 400, heightDp = 720)
@Composable
private fun TranslationEngineMissingSelectionPreview() {
    TachiyomiPreviewTheme {
        Surface {
            TranslationEnginePickerList(
                engines = previewEngineStates(),
                selected = TranslationEngineId("removed-engine"),
                density = TranslationEnginePickerDensity.Full,
                onSelect = {},
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

private val PREVIEW_ARTWORK = TranslationEngineArtwork.Bundled(android.R.drawable.ic_dialog_info)
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
