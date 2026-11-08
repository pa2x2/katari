package app.katari.entry.interactions.book.runtime

import app.katari.entry.interactions.book.document.resource.BookDocumentResourceDecoding
import app.katari.entry.interactions.book.document.resource.SkiaBookDocumentResourceDecoding
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
@BindingContainer
object BookDesktopResourceDecodingBindings {
    @Provides
    internal fun resourceDecoding(): BookDocumentResourceDecoding = SkiaBookDocumentResourceDecoding
}
