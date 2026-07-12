package mihon.entry.interactions.manga.download

import tachiyomi.core.metadata.comicinfo.ComicInfo
import tachiyomi.core.metadata.comicinfo.ComicInfoPublishingStatus
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal fun createComicInfo(
    entry: Entry,
    chapter: EntryChapter,
    urls: List<String>,
    categories: List<String>?,
    sourceName: String,
) = ComicInfo(
    title = ComicInfo.Title(chapter.name),
    series = ComicInfo.Series(entry.title),
    number = chapter.chapterNumber.takeIf { it >= 0 }?.let {
        if ((it.rem(1) == 0.0)) {
            ComicInfo.Number(it.toInt().toString())
        } else {
            ComicInfo.Number(it.toString())
        }
    },
    web = ComicInfo.Web(urls.joinToString(" ")),
    summary = entry.description?.let { ComicInfo.Summary(it) },
    writer = entry.author?.let { ComicInfo.Writer(it) },
    penciller = entry.artist?.let { ComicInfo.Penciller(it) },
    translator = chapter.scanlator?.let { ComicInfo.Translator(it) },
    genre = entry.genre?.let { ComicInfo.Genre(it.joinToString()) },
    publishingStatus = ComicInfo.PublishingStatusTachiyomi(
        ComicInfoPublishingStatus.toComicInfoValue(entry.status.value.toLong()),
    ),
    categories = categories?.let { ComicInfo.CategoriesTachiyomi(it.joinToString()) },
    source = ComicInfo.SourceMihon(sourceName),
    inker = null,
    colorist = null,
    letterer = null,
    coverArtist = null,
    tags = null,
)
