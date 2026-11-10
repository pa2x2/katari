package app.katari.ui.telemetry

import app.katari.profiles.core.ProfileDatabase
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.preference.SyncPreferences
import app.katari.telemetry.usage.AppState
import app.katari.telemetry.usage.AppStateSource
import app.katari.telemetry.usage.SyncUse
import app.katari.text.recognition.api.host.TextRecognitionHostActions
import app.katari.translation.api.host.TranslationHostActions
import app.katari.tts.api.host.TtsHostActions
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.ExtensionManagement
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.first
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.identity.SourceProviderKind

/** Reads the [AppState] the day's usage report carries from the app's own data and settings. */
@Inject
class AppStateReader(
    private val profiles: ProfileDatabase,
    private val entries: EntryRepository,
    private val extensions: ExtensionManagement,
    private val syncConnection: SyncConnection,
    private val syncPreferences: SyncPreferences,
    private val translation: TranslationHostActions,
    private val tts: TtsHostActions,
    private val textRecognition: TextRecognitionHostActions,
) : AppStateSource {
    override suspend fun read(): AppState {
        val profileIds = profiles.getProfiles(includeArchived = false).map { it.id }
        val library = profileIds.flatMap { entries.getFavoritesByProfile(it) }.groupingBy { it.type }.eachCount()
        val loaded = extensions.loaded.first().groupingBy { it.providerKind }.eachCount()
        val sync = syncConnection.state()
        return AppState(
            // Every type and kind, none of them left out for having nothing, so that averages count the zeros
            libraryEntries = EntryType.entries.associate { it.name.lowercase() to (library[it] ?: 0) },
            extensions = EXTENSION_KINDS.associate { it.key to (loaded[it] ?: 0) },
            profiles = profileIds.size,
            sync = when {
                !sync.enabled -> SyncUse.Off
                syncPreferences.paused.get() -> SyncUse.Paused
                else -> SyncUse.On
            },
            translation = translation.selectedEngine.takeIf { it.isSet() }?.get()?.value,
            readAloud = tts.knownEngines.find { it.id == tts.selectedEngine.get() }?.providerId?.value,
            textRecognition = textRecognition.observeConfiguration().first().provider?.value,
        )
    }

    private companion object {
        // The local source is built in, never an extension
        val EXTENSION_KINDS = listOf(
            SourceProviderKind.TACHIYOMI_EXTENSION,
            SourceProviderKind.KATARI_EXTENSION,
            SourceProviderKind.ANIYOMI_EXTENSION,
            SourceProviderKind.LNREADER_PLUGIN,
        )
    }
}
