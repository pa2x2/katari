package eu.kanade.presentation.entry.components.preview

import androidx.compose.ui.graphics.ImageBitmap

/**
 * What a preview tile shows of a page that arrives progressively: the frame to draw so far, or null, and whether a
 * finished replayable animation keeps playing instead of the decoded page.
 */
internal class ProgressivePreview(val image: ImageBitmap?, val keepsAnimation: Boolean)
