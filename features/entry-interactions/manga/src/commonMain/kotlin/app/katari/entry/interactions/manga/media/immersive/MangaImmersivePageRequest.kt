package app.katari.entry.interactions.manga.media.immersive

import app.katari.entry.interactions.manga.reader.image.layout.PageImageLayout
import eu.kanade.tachiyomi.ui.reader.viewer.page.PageRequest

// A page alone, whole and as it is: the feed has no spreads, crop or background settings
internal val ImmersivePageRequest = PageRequest(
    cropBorders = false,
    automaticBackground = false,
    landscape = false,
    layout = { _, _ -> PageImageLayout.Whole },
)
