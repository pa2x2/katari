package mihon.translation.ui.picker.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.OpenInNew
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.sheet.DetailsSection
import mihon.presentation.kit.sheet.DetailsSheet
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
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
        artwork = { TranslationEngineArtwork(engine.artwork, size = KatariTheme.sizes.artworkLarge) },
        status = {
            Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
                TranslationEngineStatusPill(model.status)
                model.status.explanation?.let {
                    Text(it.text(), role = TextRole.Body, tone = ContentTone.Secondary)
                }
            }
        },
    ) {
        DetailsSection(stringResource(MR.strings.translation_engine_about), engine.details.description)
        DetailsSection(stringResource(MR.strings.translation_engine_processing), engine.details.processingLocation)
        DetailsSection(stringResource(MR.strings.translation_engine_privacy), engine.details.privacyDescription)
        engine.details.artworkAttribution?.let { attribution ->
            DetailsSection(stringResource(MR.strings.translation_engine_artwork_attribution), attribution)
            engine.details.artworkAttributionUrl?.let { url ->
                Button(
                    label = stringResource(MR.strings.translation_engine_open_attribution),
                    onClick = { onOpenUrl(url) },
                    style = ButtonStyle.Text,
                    icon = MaterialSymbols.AutoMirroredRounded.OpenInNew,
                )
            }
        }
        engine.documentationUrl?.let { url ->
            Button(
                label = stringResource(MR.strings.translation_engine_open_documentation),
                onClick = { onOpenUrl(url) },
                style = ButtonStyle.Text,
                icon = MaterialSymbols.AutoMirroredRounded.OpenInNew,
            )
        }
    }
}
