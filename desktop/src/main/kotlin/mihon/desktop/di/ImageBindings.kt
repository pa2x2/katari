package mihon.desktop.di

import coil3.ImageLoader
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import mihon.desktop.image.desktopImageLoader
import mihon.desktop.storage.DesktopDirectories

@BindingContainer
object ImageBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun imageLoader(networkHelper: NetworkHelper, directories: DesktopDirectories): ImageLoader =
        desktopImageLoader(networkHelper, directories)
}
