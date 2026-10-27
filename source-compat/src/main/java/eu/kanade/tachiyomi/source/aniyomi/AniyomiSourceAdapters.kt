package eu.kanade.tachiyomi.source.aniyomi

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.source.aniyomi.capability.relatedTitlesCall
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.UnmeteredSource
import eu.kanade.tachiyomi.animesource.UnmeteredSource as AniyomiUnmeteredSource

/**
 * Wraps this Aniyomi source for Katari under [localKey], the key its identity was registered with. The adapter has
 * exactly the capabilities the source has, since Katari offers features by checking a source's type. It is
 * [AutoCloseable]: close it when the extension is unloaded.
 */
fun AnimeSource.toUnifiedSource(localKey: Long): UnifiedSource {
    val unmetered = this is AniyomiUnmeteredSource
    val related = relatedTitlesCall != null
    val web = this is AnimeHttpSource
    val settings = this is ConfigurableAnimeSource
    // Most capabilities first: the first match is the class with every capability the source has
    return when {
        unmetered && related && web && settings -> AniyomiUnmeteredRelatedWebSettingsAdapter(this, localKey)
        unmetered && related && web -> AniyomiUnmeteredRelatedWebAdapter(this, localKey)
        unmetered && related && settings -> AniyomiUnmeteredRelatedSettingsAdapter(this, localKey)
        unmetered && web && settings -> AniyomiUnmeteredWebSettingsAdapter(this, localKey)
        related && web && settings -> AniyomiRelatedWebSettingsAdapter(this, localKey)
        unmetered && related -> AniyomiUnmeteredRelatedAdapter(this, localKey)
        unmetered && web -> AniyomiUnmeteredWebAdapter(this, localKey)
        unmetered && settings -> AniyomiUnmeteredSettingsAdapter(this, localKey)
        related && web -> AniyomiRelatedWebAdapter(this, localKey)
        related && settings -> AniyomiRelatedSettingsAdapter(this, localKey)
        web && settings -> AniyomiWebSettingsAdapter(this, localKey)
        unmetered -> AniyomiUnmeteredAdapter(this, localKey)
        related -> AniyomiRelatedAdapter(this, localKey)
        web -> AniyomiWebAdapter(this, localKey)
        settings -> AniyomiSettingsAdapter(this, localKey)
        else -> AniyomiSourceAdapter(this, localKey)
    }
}

private class AniyomiSettingsAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    AniyomiSettingsBridge

private class AniyomiWebAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    AniyomiWebBridge

private class AniyomiWebSettingsAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    AniyomiWebBridge,
    AniyomiSettingsBridge

private class AniyomiRelatedAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    AniyomiRelatedBridge

private class AniyomiRelatedSettingsAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    AniyomiRelatedBridge,
    AniyomiSettingsBridge

private class AniyomiRelatedWebAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    AniyomiRelatedBridge,
    AniyomiWebBridge

private class AniyomiRelatedWebSettingsAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    AniyomiRelatedBridge,
    AniyomiWebBridge,
    AniyomiSettingsBridge

private class AniyomiUnmeteredAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    UnmeteredSource

private class AniyomiUnmeteredSettingsAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    UnmeteredSource,
    AniyomiSettingsBridge

private class AniyomiUnmeteredWebAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    UnmeteredSource,
    AniyomiWebBridge

private class AniyomiUnmeteredWebSettingsAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    UnmeteredSource,
    AniyomiWebBridge,
    AniyomiSettingsBridge

private class AniyomiUnmeteredRelatedAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    UnmeteredSource,
    AniyomiRelatedBridge

private class AniyomiUnmeteredRelatedSettingsAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    UnmeteredSource,
    AniyomiRelatedBridge,
    AniyomiSettingsBridge

private class AniyomiUnmeteredRelatedWebAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    UnmeteredSource,
    AniyomiRelatedBridge,
    AniyomiWebBridge

private class AniyomiUnmeteredRelatedWebSettingsAdapter(source: AnimeSource, id: Long) :
    AniyomiSourceAdapter(source, id),
    UnmeteredSource,
    AniyomiRelatedBridge,
    AniyomiWebBridge,
    AniyomiSettingsBridge
