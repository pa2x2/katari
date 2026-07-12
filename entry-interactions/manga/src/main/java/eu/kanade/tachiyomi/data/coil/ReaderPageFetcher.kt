package eu.kanade.tachiyomi.data.coil

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import okio.buffer
import okio.source

internal class ReaderPageFetcher(
    private val page: ReaderPage,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val stream = checkNotNull(page.stream) { "No preview stream available for page ${page.index}" }
        return SourceFetchResult(
            source = ImageSource(
                source = stream().source().buffer(),
                fileSystem = options.fileSystem,
            ),
            mimeType = null,
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<ReaderPage> {
        override fun create(
            data: ReaderPage,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher {
            return ReaderPageFetcher(data, options)
        }
    }
}
