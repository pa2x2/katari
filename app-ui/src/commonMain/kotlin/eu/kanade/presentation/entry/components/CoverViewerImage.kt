package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable

/**
 * One page of the cover viewer: the image [data] loads, fitted inside [contentPadding] and zoomable. A tap calls
 * [onClick].
 */
@Composable
internal expect fun CoverViewerImage(data: Any?, contentPadding: PaddingValues, onClick: () -> Unit)
