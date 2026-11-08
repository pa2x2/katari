package app.katari.desktop.appearance

import app.katari.core.metro.BasePreferenceStore
import app.katari.core.platform.locale.AppLocaleStore
import app.katari.desktop.application.AppRestart
import dev.zacsweers.metro.Inject
import tachiyomi.core.common.preference.PreferenceStore
import java.util.Locale

/**
 * The language picked for the app, kept on the base store: it applies before any profile opens, to every window of
 * the run. [applyAtStart] makes it the JVM's default locale, which the app's strings and formats are read in, before
 * the first window composes; text already shown can't change language, so a new pick applies once the app restarts.
 * Without a pick the default stays the system's.
 */
@Inject
class PreferenceAppLocaleStore(
    @BasePreferenceStore store: PreferenceStore,
    private val restart: AppRestart,
) : AppLocaleStore {
    // Empty while the app follows the system's language
    private val languageTag = store.getString("app_language", "")

    override val current: Locale?
        get() = Locale.getDefault()

    override val picked: Locale?
        get() = languageTag.get().takeIf { it.isNotEmpty() }?.let(Locale::forLanguageTag)

    override val appliesAtOnce: Boolean = false

    override fun set(languageTag: String?) {
        this.languageTag.set(languageTag.orEmpty())
    }

    override fun restartToApply() {
        restart.request()
    }

    fun applyAtStart() {
        picked?.let(Locale::setDefault)
    }
}
