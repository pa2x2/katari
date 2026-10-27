package mihon.text.recognition.ui.picker.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.theme.KatariTheme
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.provider.KnownTextRecognitionProvider
import mihon.text.recognition.api.provider.TextRecognitionProviderId

/** Engine cards; engines this build excludes are listed but cannot be chosen. */
@Composable
fun TextRecognitionEnginePickerList(
    providers: List<KnownTextRecognitionProvider>,
    components: List<KnownTextRecognitionComponent>,
    selected: TextRecognitionProviderId?,
    onSelect: (TextRecognitionProviderId) -> Unit,
    onOpenDocumentation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var details by remember { mutableStateOf<KnownTextRecognitionProvider?>(null) }
    RowList(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(KatariTheme.spacing.contentInset),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
    ) {
        items(providers, key = { it.id.value }) { provider ->
            TextRecognitionEngineCard(
                provider = provider,
                components = components.filter { it.provider == provider.id },
                selected = provider.id == selected,
                onSelect = { onSelect(provider.id) },
                onOpenDetails = { details = provider },
            )
        }
    }
    details?.let { provider ->
        TextRecognitionEngineDetailsSheet(
            provider = provider,
            components = components.filter { it.provider == provider.id },
            onOpenDocumentation = onOpenDocumentation,
            onDismiss = { details = null },
        )
    }
}
