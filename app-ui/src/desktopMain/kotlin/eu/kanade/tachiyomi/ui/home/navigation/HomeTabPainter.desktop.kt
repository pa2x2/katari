package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import app.katari.core.common.HomeScreenTabs
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.CollectionsBookmark
import app.katari.icons.materialsymbols.rounded.Explore
import app.katari.icons.materialsymbols.rounded.MoreVert
import app.katari.icons.materialsymbols.rounded.Schedule
import app.katari.icons.materialsymbols.rounded.Update

// Android animates these icons from its own animated vectors, which Compose Multiplatform can't load. Desktop draws
// still icons; the dock marks the selected tab.
@Composable
internal actual fun homeTabPainter(tab: HomeScreenTabs, selected: Boolean): Painter = rememberVectorPainter(
    when (tab) {
        HomeScreenTabs.Library -> MaterialSymbols.Rounded.CollectionsBookmark
        HomeScreenTabs.Updates -> MaterialSymbols.Rounded.Update
        HomeScreenTabs.History -> MaterialSymbols.Rounded.Schedule
        HomeScreenTabs.Browse -> MaterialSymbols.Rounded.Explore
        HomeScreenTabs.More -> MaterialSymbols.Rounded.MoreVert
        HomeScreenTabs.Profiles,
        HomeScreenTabs.Translator,
        HomeScreenTabs.Statistics,
        -> error("$tab has its own icon")
    },
)
