package app.katari.android.di

import app.katari.android.share.AndroidImageSharing
import app.katari.android.share.AndroidLinkSharing
import app.katari.android.share.AndroidTextSharing
import app.katari.core.platform.share.ImageSharing
import app.katari.core.platform.share.LinkSharing
import app.katari.core.platform.share.TextSharing
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds

@BindingContainer
interface ShareBindings {
    @Binds
    val AndroidImageSharing.imageSharing: ImageSharing

    @Binds
    val AndroidLinkSharing.linkSharing: LinkSharing

    @Binds
    val AndroidTextSharing.textSharing: TextSharing
}
