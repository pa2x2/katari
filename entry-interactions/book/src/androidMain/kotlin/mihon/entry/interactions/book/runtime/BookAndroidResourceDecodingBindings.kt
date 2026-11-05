package mihon.entry.interactions.book.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import mihon.entry.interactions.book.document.resource.AndroidBookDocumentResourceDecoding
import mihon.entry.interactions.book.document.resource.BookDocumentResourceDecoding

@ContributesTo(AppScope::class)
@BindingContainer
object BookAndroidResourceDecodingBindings {
    @Provides
    internal fun resourceDecoding(): BookDocumentResourceDecoding = AndroidBookDocumentResourceDecoding
}
