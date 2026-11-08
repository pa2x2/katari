package app.katari.entry.interactions.manga.media.immersive

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn

internal object AndroidMangaImmersivePlatform : MangaImmersivePlatform {
    @Composable
    override fun KeepAwake(active: Boolean) {
        if (active) Box(modifier = Modifier.keepScreenOn())
    }
}
