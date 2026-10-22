package mihon.tts.ui.picker.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.sheet.DetailsLink
import mihon.presentation.kit.sheet.DetailsSection
import mihon.presentation.kit.sheet.DetailsSheet
import mihon.presentation.kit.surface.ArtworkTileSize
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Who provides an engine, its state, and how it processes text and handles privacy. */
@Composable
internal fun TtsEngineDetailsSheet(
    model: TtsEngineCardModel,
    onDismissRequest: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val engine = model.state.engine
    DetailsSheet(
        title = engine.engineName,
        onDismissRequest = onDismissRequest,
        subtitle = stringResource(MR.strings.tts_settings_engine_provided_by, engine.providerName),
        artwork = { TtsEngineArtwork(engine.artwork, size = ArtworkTileSize.Header) },
        status = {
            Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
                TtsEngineStatusPill(model.status)
                model.status.explanation?.let {
                    Text(it, role = TextRole.Body, tone = ContentTone.Secondary)
                }
            }
        },
        links = listOfNotNull(
            engine.documentationUrl?.let { url ->
                DetailsLink(stringResource(MR.strings.tts_settings_engine_open_documentation), { onOpenUrl(url) })
            },
        ),
    ) {
        DetailsSection(stringResource(MR.strings.tts_settings_engine_about), engine.details.description)
        DetailsSection(
            stringResource(MR.strings.tts_settings_engine_processing),
            engine.details.processingDescription,
        )
        DetailsSection(stringResource(MR.strings.tts_settings_engine_privacy), engine.details.privacyDescription)
    }
}
