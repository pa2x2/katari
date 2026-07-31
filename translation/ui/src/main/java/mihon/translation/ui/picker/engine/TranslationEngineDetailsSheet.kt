package mihon.translation.ui.picker.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TranslationEngineDetailsSheet(
    model: TranslationEngineCardModel,
    onDismissRequest: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val engine = model.state.engine
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TranslationEngineArtwork(
                    artwork = engine.artwork,
                    size = 72.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = engine.engineName,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = stringResource(
                            MR.strings.translation_engine_provided_by,
                            engine.providerName,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TranslationEngineStatusPill(model.status)
            model.status.explanation?.let {
                Text(
                    text = it.text(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
            DetailsSection(
                title = stringResource(MR.strings.translation_engine_about),
                body = engine.details.description,
            )
            DetailsSection(
                title = stringResource(MR.strings.translation_engine_processing),
                body = engine.details.processingLocation,
            )
            DetailsSection(
                title = stringResource(MR.strings.translation_engine_privacy),
                body = engine.details.privacyDescription,
            )
            engine.details.artworkAttribution?.let { attribution ->
                DetailsSection(
                    title = stringResource(MR.strings.translation_engine_artwork_attribution),
                    body = attribution,
                )
                engine.details.artworkAttributionUrl?.let { url ->
                    TextButton(onClick = { onOpenUrl(url) }) {
                        Text(stringResource(MR.strings.translation_engine_open_attribution))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
            engine.documentationUrl?.let { url ->
                TextButton(onClick = { onOpenUrl(url) }) {
                    Text(stringResource(MR.strings.translation_engine_open_documentation))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            Button(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(MR.strings.translation_engine_close))
            }
        }
    }
}

@Composable
private fun DetailsSection(
    title: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
