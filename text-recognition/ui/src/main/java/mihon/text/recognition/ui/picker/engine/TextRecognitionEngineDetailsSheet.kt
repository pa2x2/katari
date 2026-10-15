package mihon.text.recognition.ui.picker.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.sheet.DetailsSection
import mihon.presentation.kit.sheet.DetailsSheet
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.provider.KnownTextRecognitionProvider
import mihon.text.recognition.ui.language.displayNames
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
    ) {
        DetailsSection(stringResource(MR.strings.text_recognition_engine_processing), provider.processingLocation)
        DetailsSection(stringResource(MR.strings.text_recognition_engine_components)) {
            Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m)) {
                components.forEach { component -> ComponentDetails(component, onOpenDocumentation) }
            }
        }
    }
}

@Composable
private fun ComponentDetails(component: KnownTextRecognitionComponent, onOpenDocumentation: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs)) {
        Text(component.displayName, role = TextRole.ItemTitle)
        Text(component.description, role = TextRole.Body, tone = ContentTone.Secondary)
        if (component.languages.isNotEmpty()) {
            Text(
                text = stringResource(MR.strings.text_recognition_engine_languages, component.languages.displayNames()),
                role = TextRole.Supporting,
            )
        }
        component.documentationUrl?.let { url ->
            Button(
                label = stringResource(MR.strings.text_recognition_engine_documentation),
                onClick = { onOpenDocumentation(url) },
                style = ButtonStyle.Text,
            )
        }
    }
}
