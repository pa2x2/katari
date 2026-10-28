package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.app.share.AndroidImageSharing
import mihon.app.share.AndroidLinkSharing
import mihon.core.platform.share.ImageSharing
import mihon.core.platform.share.LinkSharing

@BindingContainer
interface ShareBindings {
    @Binds
    val AndroidImageSharing.imageSharing: ImageSharing

    @Binds
    val AndroidLinkSharing.linkSharing: LinkSharing
}
