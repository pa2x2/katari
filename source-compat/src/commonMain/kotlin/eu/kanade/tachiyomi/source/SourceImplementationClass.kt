package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.adapter.LegacyMangaSourceAdapter
import eu.kanade.tachiyomi.source.aniyomi.AniyomiSourceHolder
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.failure.containExtensionCode
import eu.kanade.tachiyomi.source.failure.containedSource
import kotlin.reflect.KClass

/**
 * The class the source was written as, which enhanced trackers such as Komga recognise their own sources by. No source
 * reaches the app as that class: Tachiyomi and Aniyomi sources come inside an adapter and Entry SDK sources behind
 * [containExtensionCode]'s proxy, so the source's own class names the wrapper. Sources Katari writes itself, such as
 * the local source and LNReader plugins' sources, are their own class.
 */
val UnifiedSource.implementationClass: KClass<*>
    get() = when (this) {
        is LegacyMangaSourceAdapter -> source::class
        is AniyomiSourceHolder -> source::class
        else -> containedSource::class
    }
