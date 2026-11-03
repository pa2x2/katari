package mihon.presentation.kit.layout

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Android windows are touch windows, which show a list's scroll indicator instead and never ask for a scrollbar

@Composable
internal actual fun ListScrollbar(state: LazyListState, modifier: Modifier) = Unit

@Composable
internal actual fun GridScrollbar(state: LazyGridState, modifier: Modifier) = Unit

@Composable
internal actual fun RowScrollbar(state: LazyListState, modifier: Modifier) = Unit

@Composable
internal actual fun RowScrollbar(state: ScrollState, modifier: Modifier) = Unit
