package mihon.tts.ui.picker.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.tts.api.engine.TtsEngineId
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Explains that the chosen engine is no longer installed or registered. */
@Composable
internal fun TtsMissingEngineCard(selected: TtsEngineId, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        container = SurfaceContainer.ErrorContainer,
        shape = KatariTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(KatariTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            Text(stringResource(MR.strings.tts_settings_engine_unavailable), role = TextRole.SectionTitle)
            Text(stringResource(MR.strings.tts_settings_engine_missing_id, selected.value), role = TextRole.Body)
            Text(stringResource(MR.strings.tts_settings_engine_missing_explanation), role = TextRole.Body)
        }
    }
}
