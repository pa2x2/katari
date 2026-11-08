package app.katari.text.recognition.ui.picker.engine

import androidx.compose.runtime.Composable
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.sheet.DetailsLink
import app.katari.presentation.kit.sheet.DetailsSection
import app.katari.presentation.kit.sheet.DetailsSheet
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.TextRole
import app.katari.text.recognition.api.component.KnownTextRecognitionComponent
import app.katari.text.recognition.api.provider.KnownTextRecognitionProvider
import app.katari.text.recognition.ui.language.displayNames
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Where an engine processes pages and which components it combines. */
@Composable
internal fun TextRecognitionEngineDetailsSheet(
    provider: KnownTextRecognitionProvider,
    components: List<KnownTextRecognitionComponent>,
    onOpenDocumentation: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    DetailsSheet(
        title = provider.name,
        onDismissRequest = onDismiss,
        subtitle = provider.description,
        links = components.mapNotNull { component ->
            component.documentationUrl?.let { url ->
                DetailsLink(
                    label = stringResource(MR.strings.text_recognition_engine_documentation),
                    onClick = { onOpenDocumentation(url) },
                    supportingLabel = component.displayName,
                )
            }
        },
    ) {
        DetailsSection(stringResource(MR.strings.text_recognition_engine_processing), provider.processingLocation)
        components.forEach { component -> ComponentDetails(component) }
    }
}

@Composable
private fun ComponentDetails(component: KnownTextRecognitionComponent) {
    DetailsSection(component.displayName) {
        Text(component.description, role = TextRole.Body, tone = ContentTone.Secondary)
        if (component.languages.isNotEmpty()) {
            Text(
                text = stringResource(MR.strings.text_recognition_engine_languages, component.languages.displayNames()),
                role = TextRole.Supporting,
            )
        }
    }
}
