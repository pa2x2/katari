package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.app.share.AndroidImageSharing
import mihon.core.platform.share.ImageSharing

@BindingContainer
interface ShareBindings {
    @Binds
    val AndroidImageSharing.imageSharing: ImageSharing
}
