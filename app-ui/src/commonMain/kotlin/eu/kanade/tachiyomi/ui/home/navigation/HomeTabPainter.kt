package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import mihon.core.common.HomeScreenTabs

/** The icon of the Library, Updates, History, Browse or More tab, which marks whether the tab is [selected]. */
@Composable
internal expect fun homeTabPainter(tab: HomeScreenTabs, selected: Boolean): Painter
