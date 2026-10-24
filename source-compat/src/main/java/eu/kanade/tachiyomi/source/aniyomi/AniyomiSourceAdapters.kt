package eu.kanade.tachiyomi.source.aniyomi

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.UnmeteredSource
import eu.kanade.tachiyomi.animesource.UnmeteredSource as AniyomiUnmeteredSource

/**
 * Wraps this Aniyomi source for Katari under [localKey], the key its identity was registered with. The adapter has
 * exactly the capabilities the source has, since Katari offers features by checking a source's type.
 */
fun AnimeSource.toUnifiedSource(localKey: Long): UnifiedSource {
    val web = this is AnimeHttpSource
    val settings = this is ConfigurableAnimeSource
    val unmetered = this is AniyomiUnmeteredSource
    return when {
        web && settings && unmetered -> AniyomiUnmeteredWebSettingsAdapter(this, localKey)
        web && settings -> AniyomiWebSettingsAdapter(this, localKey)
        web && unmetered -> AniyomiUnmeteredWebAdapter(this, localKey)
        web -> AniyomiWebAdapter(this, localKey)
        settings && unmetered -> AniyomiUnmeteredSettingsAdapter(this, localKey)
        settings -> AniyomiSettingsAdapter(this, localKey)
        unmetered -> AniyomiUnmeteredAdapter(this, localKey)
        else -> AniyomiSourceAdapter(this, localKey)
    }
}

private class AniyomiWebAdapter(source: AnimeSource, id: Long) : AniyomiSourceAdapter(source, id), AniyomiWebBridge

private class AniyomiSettingsAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    AniyomiSettingsBridge

private class AniyomiWebSettingsAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    AniyomiWebBridge,
    AniyomiSettingsBridge

private class AniyomiUnmeteredAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    UnmeteredSource

private class AniyomiUnmeteredWebAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    AniyomiWebBridge,
    UnmeteredSource

private class AniyomiUnmeteredSettingsAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    AniyomiSettingsBridge,
    UnmeteredSource

private class AniyomiUnmeteredWebSettingsAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    AniyomiWebBridge,
    AniyomiSettingsBridge,
    UnmeteredSource
