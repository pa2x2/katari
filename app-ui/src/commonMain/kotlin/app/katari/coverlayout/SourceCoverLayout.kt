package app.katari.coverlayout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import app.katari.presentation.kit.layout.CoverSizing
import app.katari.presentation.kit.layout.rememberCoverSizing
import eu.kanade.domain.source.model.SourceLayout
import eu.kanade.domain.source.service.SourcePreferences

/** How [sourceId]'s covers are laid out, Browse's layout or its own, as it changes. */
@Composable
fun collectSourceLayout(preferences: SourcePreferences, sourceId: Long): State<SourceLayout> =
    remember(preferences, sourceId) { preferences.sourceLayout(sourceId) }
        .collectAsState(initial = remember(preferences, sourceId) { preferences.getSourceLayout(sourceId) })

/** The cover size of [sourceId]'s grid at [layout], stored where [layout] comes from as the grid resizes it. */
@Composable
fun rememberSourceCoverSizing(preferences: SourcePreferences, sourceId: Long, layout: SourceLayout): CoverSizing =
    rememberCoverSizing(layout.coverSize) { preferences.setSourceCoverSize(sourceId, it) }

/** The Display panel of one source's covers, named [sourceName], which changes Browse's layout or the source's own. */
@Composable
fun SourceCoverLayoutSettings(
    preferences: SourcePreferences,
    sourceId: Long,
    sourceName: String,
    layout: SourceLayout,
    sizing: CoverSizing,
) {
    val ownMode = preferences.sourceDisplayMode(sourceId)
    CoverLayoutSettings(
        displayMode = layout.displayMode,
        onDisplayModeChange = { preferences.setSourceDisplayMode(sourceId, it) },
        sizing = sizing,
        displayModeSetting = if (layout.ownLayout) ownMode else preferences.sourceDisplayMode,
        coverSizeSetting = if (layout.ownLayout) preferences.sourceCoverSize(sourceId) else preferences.browseCoverSize,
        ownLayout = SourceOwnLayout(
            sourceName = sourceName,
            own = layout.ownLayout,
            onOwnChange = { preferences.setSourceOwnLayout(sourceId, it) },
            setting = ownMode,
        ),
    )
}
