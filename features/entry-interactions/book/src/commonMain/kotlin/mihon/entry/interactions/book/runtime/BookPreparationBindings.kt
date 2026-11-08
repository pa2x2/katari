package mihon.entry.interactions.book.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import mihon.core.platform.storage.AppPrivateDirectories
import mihon.entry.interactions.book.content.BookMaterializationCache
import mihon.entry.interactions.book.content.BookMaterializationStore
import mihon.entry.interactions.book.document.preparation.BookDocumentPreparedCache
import mihon.entry.interactions.book.document.resource.BookPublicationResourceGatewayFactory
import mihon.entry.interactions.book.format.epub.preparation.EpubBookPreparer
import mihon.entry.interactions.book.format.html.prosechapter.preparation.HtmlProseChapterPreparer
import mihon.entry.interactions.book.preparation.BookContentPreparerRegistry

/** Publication preparation and its caches, which the reader and the downloader share. */
@ContributesTo(AppScope::class)
@BindingContainer
object BookPreparationBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun materializationCache(directories: AppPrivateDirectories): BookMaterializationCache =
        BookMaterializationCache(directories)

    @Provides
    internal fun materializationStore(cache: BookMaterializationCache): BookMaterializationStore = cache

    @Provides
    @SingleIn(AppScope::class)
    internal fun preparedDocumentCache(directories: AppPrivateDirectories): BookDocumentPreparedCache =
        BookDocumentPreparedCache(directories)

    @Provides
    @SingleIn(AppScope::class)
    internal fun resourceGatewayFactory(
        directories: AppPrivateDirectories,
        networkHelper: NetworkHelper,
    ): BookPublicationResourceGatewayFactory = BookPublicationResourceGatewayFactory(directories, networkHelper.client)

    @Provides
    @SingleIn(AppScope::class)
    internal fun preparerRegistry(
        preparedDocumentCache: BookDocumentPreparedCache,
        resourceGatewayFactory: BookPublicationResourceGatewayFactory,
    ): BookContentPreparerRegistry = BookContentPreparerRegistry(
        preparers = listOf(
            HtmlProseChapterPreparer(),
            EpubBookPreparer(preparedDocumentCache, resourceGatewayFactory),
        ),
    )
}
