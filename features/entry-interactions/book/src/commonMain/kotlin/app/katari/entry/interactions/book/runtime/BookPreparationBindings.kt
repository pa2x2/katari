package app.katari.entry.interactions.book.runtime

import app.katari.core.platform.storage.AppPrivateDirectories
import app.katari.entry.interactions.book.content.BookMaterializationCache
import app.katari.entry.interactions.book.content.BookMaterializationStore
import app.katari.entry.interactions.book.document.preparation.BookDocumentPreparedCache
import app.katari.entry.interactions.book.document.resource.BookPublicationResourceGatewayFactory
import app.katari.entry.interactions.book.format.epub.preparation.EpubBookPreparer
import app.katari.entry.interactions.book.format.html.prosechapter.preparation.HtmlProseChapterPreparer
import app.katari.entry.interactions.book.preparation.BookContentPreparerRegistry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper

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
