package app.katari.translation.ui.picker.engine

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

/** Who provides an engine, its state, where it processes text, how it handles privacy, and artwork credits. */
@Composable
internal fun TranslationEngineDetailsSheet(
    model: TranslationEngineCardModel,
    onDismissRequest: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val engine = model.state.engine
    DetailsSheet(
        title = engine.engineName,
        onDismissRequest = onDismissRequest,
        subtitle = stringResource(MR.strings.translation_engine_provided_by, engine.providerName),
        artwork = { TranslationEngineArtwork(engine.artwork, size = ArtworkTileSize.Header) },
        status = {
            Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
                TranslationEngineStatusPill(model.status)
                model.status.explanation?.let {
                    Text(it.text(), role = TextRole.Body, tone = ContentTone.Secondary)
                }
            }
        },
        links = listOfNotNull(
            engine.details.artworkAttributionUrl?.takeIf { engine.details.artworkAttribution != null }?.let { url ->
                DetailsLink(stringResource(MR.strings.translation_engine_open_attribution), { onOpenUrl(url) })
            },
            engine.documentationUrl?.let { url ->
                DetailsLink(stringResource(MR.strings.translation_engine_open_documentation), { onOpenUrl(url) })
            },
        ),
    ) {
        DetailsSection(stringResource(MR.strings.translation_engine_about), engine.details.description)
        DetailsSection(stringResource(MR.strings.translation_engine_processing), engine.details.processingLocation)
        DetailsSection(stringResource(MR.strings.translation_engine_privacy), engine.details.privacyDescription)
        engine.details.artworkAttribution?.let { attribution ->
            DetailsSection(stringResource(MR.strings.translation_engine_artwork_attribution), attribution)
        }
    }
}
