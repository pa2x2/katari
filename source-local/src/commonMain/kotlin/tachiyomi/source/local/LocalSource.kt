package tachiyomi.source.local

import eu.kanade.tachiyomi.source.entry.EmptyChapterListSource
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.UnmeteredSource

expect class LocalSource : EntryCatalogueSource, EmptyChapterListSource, UnmeteredSource
