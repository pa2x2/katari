package eu.kanade.tachiyomi.source.aniyomi.entry

/*
 * Some extensions list a title's own episodes among its seasons, under the title's URL: FilmiFen opens a series on its
 * first season's page and lists that page as season 1. A merged entry has no chapters of its own and can't list itself,
 * so Katari stores that season under the title's URL with a suffix the extension never sees.
 */

private const val OWN_EPISODES_SUFFIX = "#own-episodes"

/** The URL Katari stores the episodes the title at [titleUrl] lists as its own season under. */
internal fun ownEpisodesUrl(titleUrl: String): String = titleUrl + OWN_EPISODES_SUFFIX

/** [storedUrl] as the extension knows it. */
internal fun extensionUrl(storedUrl: String): String = storedUrl.removeSuffix(OWN_EPISODES_SUFFIX)
