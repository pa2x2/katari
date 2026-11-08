package app.katari.entry.interactions.book.runtime

import app.katari.entry.interactions.book.document.resource.AndroidBookDocumentResourceDecoding
import app.katari.entry.interactions.book.document.resource.BookDocumentResourceDecoding
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
@BindingContainer
object BookAndroidResourceDecodingBindings {
    @Provides
    internal fun resourceDecoding(): BookDocumentResourceDecoding = AndroidBookDocumentResourceDecoding
}
