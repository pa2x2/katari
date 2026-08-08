package mihon.entry.interactions.manga.page.acquisition

import kotlinx.coroutines.CancellationException

/** Cancels cache-warming work when a visible page needs the loader. */
internal class PreemptedMangaPagePreload : CancellationException()
