package eu.kanade.presentation.more.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.more.settings.LocalPreferenceHighlighted
import mihon.presentation.kit.surface.pulsingHighlight

/** Pulses the preference that settings search opened this screen for. */
@Composable
internal fun Modifier.preferenceHighlight(): Modifier =
    pulsingHighlight(Unit.takeIf { LocalPreferenceHighlighted.current })
