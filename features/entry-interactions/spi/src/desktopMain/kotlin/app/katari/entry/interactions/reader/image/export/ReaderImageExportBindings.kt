package app.katari.entry.interactions.reader.image.export

import app.katari.core.platform.clipboard.ImageClipboard
import app.katari.core.platform.storage.UserPicturesFolder
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
@BindingContainer
object ReaderImageExportBindings {
    @Provides
    internal fun imageExport(pictures: UserPicturesFolder, clipboard: ImageClipboard): ReaderImageExport =
        DesktopReaderImageExport(pictures, clipboard)
}
