package eu.kanade.presentation.more.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.surface.pulsingHighlight
import eu.kanade.presentation.more.settings.LocalPreferenceHighlighted

/** Pulses the preference that settings search opened this screen for. */
@Composable
fun Modifier.preferenceHighlight(): Modifier =
    pulsingHighlight(Unit.takeIf { LocalPreferenceHighlighted.current })
