package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.data.coil.AndroidExtensionIconSource
import eu.kanade.tachiyomi.data.coil.AndroidImageLoaderPlatform
import eu.kanade.tachiyomi.data.coil.ExtensionIconSource
import eu.kanade.tachiyomi.data.coil.ImageLoaderPlatform

@BindingContainer
interface ImageBindings {
    @Binds
    val AndroidImageLoaderPlatform.imageLoaderPlatform: ImageLoaderPlatform

    @Binds
    val AndroidExtensionIconSource.extensionIconSource: ExtensionIconSource
}
