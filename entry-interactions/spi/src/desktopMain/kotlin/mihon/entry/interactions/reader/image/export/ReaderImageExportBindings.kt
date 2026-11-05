package mihon.entry.interactions.reader.image.export

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import mihon.core.platform.clipboard.ImageClipboard
import mihon.core.platform.storage.UserPicturesFolder

@ContributesTo(AppScope::class)
@BindingContainer
object ReaderImageExportBindings {
    @Provides
    internal fun imageExport(pictures: UserPicturesFolder, clipboard: ImageClipboard): ReaderImageExport =
        DesktopReaderImageExport(pictures, clipboard)
}
