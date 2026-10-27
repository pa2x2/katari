package tachiyomi.core.common

object Constants {
    const val URL_REPOSITORY = "https://github.com/pa2x2/katari"

    // The website's docs. Their addresses and section anchors must keep working once published, because released
    // versions of the app keep opening them: a page that moves leaves a redirect behind, as /guide/ did.
    const val URL_DOCS = "https://pa2x2.github.io/katari/docs/"
    const val URL_HELP = URL_DOCS
    const val URL_HELP_TROUBLESHOOTING = "${URL_DOCS}troubleshooting/"
    const val URL_HELP_UPCOMING = "${URL_DOCS}library-updates/#upcoming"

    const val ENTRY_EXTRA = "entry"
    const val PROFILE_EXTRA = "profile"

    const val MAIN_ACTIVITY = "eu.kanade.tachiyomi.ui.main.MainActivity"

    // Shortcut actions
    const val SHORTCUT_LIBRARY = "eu.kanade.tachiyomi.SHOW_LIBRARY"
    const val SHORTCUT_MANGA = "eu.kanade.tachiyomi.SHOW_MANGA"
    const val SHORTCUT_ANIME = "eu.kanade.tachiyomi.SHOW_ANIME"
    const val SHORTCUT_UPDATES = "eu.kanade.tachiyomi.SHOW_RECENTLY_UPDATED"
    const val SHORTCUT_HISTORY = "eu.kanade.tachiyomi.SHOW_RECENTLY_READ"
    const val SHORTCUT_SOURCES = "eu.kanade.tachiyomi.SHOW_CATALOGUES"
    const val SHORTCUT_EXTENSIONS = "eu.kanade.tachiyomi.EXTENSIONS"
    const val SHORTCUT_DOWNLOADS = "eu.kanade.tachiyomi.SHOW_DOWNLOADS"
}
