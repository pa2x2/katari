package eu.kanade.tachiyomi.ui.browse.source.browse.preset

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import eu.kanade.domain.source.model.BUILTIN_LATEST_PRESET_ID
import eu.kanade.domain.source.model.BUILTIN_POPULAR_PRESET_ID
import eu.kanade.domain.source.model.SourceFeedPreset
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The name to show for a preset: built-in presets are named in the app's language, custom ones as the user saved them. */
@Composable
@ReadOnlyComposable
fun SourceFeedPreset.displayName(): String {
    return when (id) {
        BUILTIN_POPULAR_PRESET_ID -> stringResource(MR.strings.popular)
        BUILTIN_LATEST_PRESET_ID -> stringResource(MR.strings.latest)
        else -> name
    }
}
