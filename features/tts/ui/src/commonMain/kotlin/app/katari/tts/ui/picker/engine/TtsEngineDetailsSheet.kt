package app.katari.tts.ui.picker.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.sheet.DetailsLink
import app.katari.presentation.kit.sheet.DetailsSection
import app.katari.presentation.kit.sheet.DetailsSheet
import app.katari.presentation.kit.surface.ArtworkTileSize
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
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
