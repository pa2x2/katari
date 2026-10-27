package mihon.translation.ui.picker.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.translation.api.engine.TranslationEngineId
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Explains that the stored engine is no longer installed or registered. */
@Composable
internal fun TranslationMissingEngineCard(selected: TranslationEngineId, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), container = SurfaceContainer.ErrorContainer) {
        Column(
            modifier = Modifier.padding(KatariTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            Text(stringResource(MR.strings.translation_engine_missing_title), role = TextRole.SectionTitle)
            Text(stringResource(MR.strings.translation_engine_missing_id, selected.value), role = TextRole.Body)
            Text(stringResource(MR.strings.translation_engine_missing_explanation), role = TextRole.Body)
        }
    }
}
