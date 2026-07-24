package eu.kanade.tachiyomi.data.backup.models.compatibility

import eu.kanade.tachiyomi.data.backup.models.BackupDownloadPreferences
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupEntryFeatureState
import eu.kanade.tachiyomi.data.backup.models.BackupEntryProgressState
import eu.kanade.tachiyomi.data.backup.models.BackupPlaybackPreferences
import eu.kanade.tachiyomi.data.backup.models.BackupPlaybackState
import eu.kanade.tachiyomi.data.backup.models.BackupTracking
import eu.kanade.tachiyomi.data.backup.models.BackupViewerSettingOverride
import eu.kanade.tachiyomi.data.backup.models.toBackupEntryProgressState
import eu.kanade.tachiyomi.data.backup.models.toEntryProgressStateSnapshot
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.ENTRY_CHILD_GROUP_FILTER_BACKUP_SCHEMA_VERSION
import mihon.entry.interactions.ENTRY_CHILD_GROUP_FILTER_BACKUP_STATE_ID
import mihon.entry.interactions.ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_SCHEMA_VERSION
import mihon.entry.interactions.ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_STATE_ID
import mihon.entry.interactions.ENTRY_MERGE_BACKUP_SCHEMA_VERSION
import mihon.entry.interactions.ENTRY_MERGE_BACKUP_STATE_ID
import mihon.entry.interactions.ENTRY_PLAYBACK_PREFERENCES_BACKUP_SCHEMA_VERSION
import mihon.entry.interactions.ENTRY_PLAYBACK_PREFERENCES_BACKUP_STATE_ID
import mihon.entry.interactions.ENTRY_PROGRESS_BACKUP_SCHEMA_VERSION
import mihon.entry.interactions.ENTRY_PROGRESS_BACKUP_STATE_ID
import mihon.entry.interactions.ENTRY_TRACKING_BACKUP_SCHEMA_VERSION
import mihon.entry.interactions.ENTRY_TRACKING_BACKUP_STATE_ID
import mihon.entry.interactions.ENTRY_VIEWER_SETTINGS_BACKUP_SCHEMA_VERSION
import mihon.entry.interactions.ENTRY_VIEWER_SETTINGS_BACKUP_STATE_ID
import mihon.entry.interactions.EntryBackupStateCodec
import mihon.entry.interactions.EntryChildGroupFilterBackupState
import mihon.entry.interactions.EntryDownloadConfigurationBackupState
import mihon.entry.interactions.EntryDownloadConfigurationQualityMode
import mihon.entry.interactions.EntryFeatureStateEnvelope
import mihon.entry.interactions.EntryMergeBackupIdentity
import mihon.entry.interactions.EntryMergeBackupMember
import mihon.entry.interactions.EntryPlaybackPreferencesSnapshot
import mihon.entry.interactions.EntryPlaybackQualityMode
import mihon.entry.interactions.EntryProgressSnapshot
import mihon.entry.interactions.EntryProgressStateSnapshot
import mihon.entry.interactions.EntryTrackingBackupRecord
import mihon.entry.interactions.EntryTrackingBackupState
import mihon.entry.interactions.EntryViewerSettingBackupValue
import mihon.entry.interactions.EntryViewerSettingsBackupState
import mihon.entry.interactions.reader.settings.MangaReaderSettingsProvider
import mihon.entry.interactions.reader.settings.ReaderOrientation
import mihon.entry.interactions.reader.settings.ReadingMode
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryProgressLocator

private const val LEGACY_MANGA_VIEWER_MASK = 0x3FL
private const val MAX_VIEWER_SETTING_VALUE_LENGTH = 16_384

/** Finite bridge for typed fields written before Feature-state envelopes existed. */
internal fun BackupEntry.featureStatesWithLegacyFallback(entry: Entry): List<EntryFeatureStateEnvelope> {
    val states = featureStates.mapTo(mutableListOf()) { it.toEnvelope() }

    states.addIfAbsent(ENTRY_VIEWER_SETTINGS_BACKUP_STATE_ID) {
        legacyViewerSettings(entry)?.envelope(
            ENTRY_VIEWER_SETTINGS_BACKUP_STATE_ID,
            ENTRY_VIEWER_SETTINGS_BACKUP_SCHEMA_VERSION,
        )
    }
    states.addIfAbsent(ENTRY_PLAYBACK_PREFERENCES_BACKUP_STATE_ID) {
        playbackPreferences?.toState()?.envelope(
            ENTRY_PLAYBACK_PREFERENCES_BACKUP_STATE_ID,
            ENTRY_PLAYBACK_PREFERENCES_BACKUP_SCHEMA_VERSION,
        )
    }
    states.addIfAbsent(ENTRY_PROGRESS_BACKUP_STATE_ID) {
        legacyProgress(entry)?.envelope(ENTRY_PROGRESS_BACKUP_STATE_ID, ENTRY_PROGRESS_BACKUP_SCHEMA_VERSION)
    }
    states.addIfAbsent(ENTRY_CHILD_GROUP_FILTER_BACKUP_STATE_ID) {
        excludedScanlators.takeIf(List<String>::isNotEmpty)
            ?.let { EntryChildGroupFilterBackupState(it.toSet()) }
            ?.envelope(ENTRY_CHILD_GROUP_FILTER_BACKUP_STATE_ID, ENTRY_CHILD_GROUP_FILTER_BACKUP_SCHEMA_VERSION)
    }
    states.addIfAbsent(ENTRY_MERGE_BACKUP_STATE_ID) {
        legacyMerge()?.envelope(ENTRY_MERGE_BACKUP_STATE_ID, ENTRY_MERGE_BACKUP_SCHEMA_VERSION)
    }
    states.addIfAbsent(ENTRY_TRACKING_BACKUP_STATE_ID) {
        tracking.takeIf(List<BackupTracking>::isNotEmpty)
            ?.map(BackupTracking::toState)
            ?.let(::EntryTrackingBackupState)
            ?.envelope(ENTRY_TRACKING_BACKUP_STATE_ID, ENTRY_TRACKING_BACKUP_SCHEMA_VERSION)
    }
    states.addIfAbsent(ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_STATE_ID) {
        downloadPreferences?.toState()?.envelope(
            ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_STATE_ID,
            ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_SCHEMA_VERSION,
        )
    }
    return states
}

/** Writes current known state into legacy fields without influencing participant discovery. */
internal fun BackupEntry.applyLegacyFeatureStateProjection(states: List<EntryFeatureStateEnvelope>) {
    featureStates = states.map(EntryFeatureStateEnvelope::toWire)
    states.associateBy(EntryFeatureStateEnvelope::participantId).forEach { (participantId, envelope) ->
        when (participantId) {
            ENTRY_VIEWER_SETTINGS_BACKUP_STATE_ID -> {
                viewerSettingOverrides = envelope.decode<EntryViewerSettingsBackupState>().overrides.map { value ->
                    BackupViewerSettingOverride(
                        providerId = value.providerId,
                        settingKey = value.settingKey,
                        encodedValue = value.encodedValue,
                        updatedAt = value.updatedAt,
                    )
                }
            }
            ENTRY_PLAYBACK_PREFERENCES_BACKUP_STATE_ID -> {
                playbackPreferences = envelope.decode<EntryPlaybackPreferencesSnapshot>().toWire()
            }
            ENTRY_PROGRESS_BACKUP_STATE_ID -> {
                progressStates = envelope.decode<EntryProgressSnapshot>().states.map { it.toBackupEntryProgressState() }
            }
            ENTRY_CHILD_GROUP_FILTER_BACKUP_STATE_ID -> {
                excludedScanlators = envelope.decode<EntryChildGroupFilterBackupState>().excludedGroups.toList()
            }
            ENTRY_MERGE_BACKUP_STATE_ID -> {
                val state = envelope.decode<EntryMergeBackupMember>()
                mergeTargetSource = state.target.sourceId
                mergeTargetUrl = state.target.url
                mergeTargetType = state.target.type
                mergePosition = state.position
            }
            ENTRY_TRACKING_BACKUP_STATE_ID -> {
                tracking = envelope.decode<EntryTrackingBackupState>().records.map(EntryTrackingBackupRecord::toWire)
            }
            ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_STATE_ID -> {
                downloadPreferences = envelope.decode<EntryDownloadConfigurationBackupState>().toWire()
            }
        }
    }
}

internal fun BackupEntry.normalizeLegacyViewerFlags(entry: Entry): Entry {
    return if (entry.type == EntryType.MANGA) {
        entry.copy(viewerFlags = entry.viewerFlags and LEGACY_MANGA_VIEWER_MASK.inv())
    } else {
        entry
    }
}

private inline fun <reified T> EntryFeatureStateEnvelope.decode(): T {
    return EntryBackupStateCodec.decode(kotlinx.serialization.serializer(), payload)
}

private inline fun <reified T> T.envelope(participantId: String, schemaVersion: Int): EntryFeatureStateEnvelope {
    return EntryFeatureStateEnvelope(
        participantId = participantId,
        schemaVersion = schemaVersion,
        payload = EntryBackupStateCodec.encode(kotlinx.serialization.serializer(), this),
    )
}

private fun MutableList<EntryFeatureStateEnvelope>.addIfAbsent(
    participantId: String,
    state: () -> EntryFeatureStateEnvelope?,
) {
    if (none { it.participantId == participantId }) state()?.let(::add)
}

private fun BackupEntryFeatureState.toEnvelope() = EntryFeatureStateEnvelope(participantId, schemaVersion, payload)

private fun EntryFeatureStateEnvelope.toWire() = BackupEntryFeatureState(participantId, schemaVersion, payload)

private fun BackupEntry.legacyViewerSettings(entry: Entry): EntryViewerSettingsBackupState? {
    val values = viewerSettingOverrides.mapNotNull { override ->
        if (override.encodedValue.length > MAX_VIEWER_SETTING_VALUE_LENGTH) return@mapNotNull null
        if (override.providerId.isBlank() || override.settingKey.isBlank()) return@mapNotNull null
        EntryViewerSettingBackupValue(
            providerId = override.providerId,
            settingKey = override.settingKey,
            encodedValue = override.encodedValue,
            updatedAt = override.updatedAt,
        )
    }.toMutableList()
    if (entry.type == EntryType.MANGA) {
        val restoredIds = values.mapTo(mutableSetOf()) { it.providerId to it.settingKey }
        val readingMode = viewerFlags and ReadingMode.MASK.toLong()
        val readingModeId = MangaReaderSettingsProvider.PROVIDER_ID to MangaReaderSettingsProvider.READING_MODE_KEY
        if (readingMode != ReadingMode.DEFAULT.flagValue.toLong() && readingModeId !in restoredIds) {
            values +=
                EntryViewerSettingBackupValue(readingModeId.first, readingModeId.second, readingMode.toString(), 0)
        }
        val orientation = viewerFlags and ReaderOrientation.MASK.toLong()
        val orientationId = MangaReaderSettingsProvider.PROVIDER_ID to MangaReaderSettingsProvider.ORIENTATION_KEY
        if (orientation != ReaderOrientation.DEFAULT.flagValue.toLong() && orientationId !in restoredIds) {
            values +=
                EntryViewerSettingBackupValue(orientationId.first, orientationId.second, orientation.toString(), 0)
        }
    }
    return values.takeIf(List<EntryViewerSettingBackupValue>::isNotEmpty)?.let(::EntryViewerSettingsBackupState)
}

private fun BackupEntry.legacyProgress(entry: Entry): EntryProgressSnapshot? {
    val generic = progressStates.map(BackupEntryProgressState::toEntryProgressStateSnapshot)
    val identities = generic.mapTo(hashSetOf()) { it.contentKey to it.resourceKey }
    val playback = playbackStates.mapNotNull(BackupPlaybackState::toProgressState)
        .filterNot { (it.contentKey to it.resourceKey) in identities }
    val manga = if (entry.type == EntryType.MANGA) {
        val historyByUrl = history.associateBy { it.url }
        chapters.mapNotNull { chapter ->
            if (chapter.url.isBlank() || (!chapter.read && chapter.lastPageRead <= 0L)) return@mapNotNull null
            val timestamp = historyByUrl[chapter.url]?.lastRead?.coerceAtLeast(0L) ?: 0L
            EntryProgressStateSnapshot(
                resourceKey = chapter.url,
                sourceChildKey = chapter.url,
                locator = EntryProgressLocator(kind = "page", position = chapter.lastPageRead.takeIf { it > 0L }),
                completed = chapter.read,
                locatorUpdatedAt = timestamp,
                completionUpdatedAt = timestamp,
            )
        }.filterNot { (it.contentKey to it.resourceKey) in identities }
    } else {
        emptyList()
    }
    return (playback + manga + generic).takeIf(
        List<EntryProgressStateSnapshot>::isNotEmpty,
    )?.let(::EntryProgressSnapshot)
}

private fun BackupPlaybackState.toProgressState(): EntryProgressStateSnapshot? {
    if (url.isBlank() || (!completed && positionMs <= 0L)) return null
    val position = positionMs.coerceAtLeast(0L)
    val duration = durationMs.takeIf { it > 0L }
    val timestamp = lastWatchedAt.coerceAtLeast(0L)
    return EntryProgressStateSnapshot(
        resourceKey = url,
        sourceChildKey = url,
        locator = EntryProgressLocator(
            kind = "time",
            position = position,
            extent = duration,
            progression = duration?.let { (position.toDouble() / it).coerceIn(0.0, 1.0) },
        ),
        completed = completed,
        locatorUpdatedAt = timestamp,
        completionUpdatedAt = timestamp,
    )
}

private fun BackupEntry.legacyMerge(): EntryMergeBackupMember? {
    val source = mergeTargetSource ?: return null
    val url = mergeTargetUrl ?: return null
    val position = mergePosition ?: return null
    return EntryMergeBackupMember(EntryMergeBackupIdentity(source, url, mergeTargetType ?: type), position)
}

private fun BackupPlaybackPreferences.toState() = EntryPlaybackPreferencesSnapshot(
    dubKey = dubKey,
    streamKey = streamKey,
    sourceQualityKey = sourceQualityKey,
    subtitleKey = subtitleKey,
    playerQualityMode = if (playerQualityMode == "specific_height") {
        EntryPlaybackQualityMode.SPECIFIC_HEIGHT
    } else {
        EntryPlaybackQualityMode.AUTO
    },
    playerQualityHeight = playerQualityHeight,
    subtitleOffsetX = subtitleOffsetX,
    subtitleOffsetY = subtitleOffsetY,
    subtitleTextSize = subtitleTextSize,
    subtitleTextColor = subtitleTextColor,
    subtitleBackgroundColor = subtitleBackgroundColor,
    subtitleBackgroundOpacity = subtitleBackgroundOpacity,
    updatedAt = updatedAt,
)

private fun EntryPlaybackPreferencesSnapshot.toWire() = BackupPlaybackPreferences(
    dubKey = dubKey,
    streamKey = streamKey,
    sourceQualityKey = sourceQualityKey,
    subtitleKey = subtitleKey,
    playerQualityMode = if (playerQualityMode ==
        EntryPlaybackQualityMode.SPECIFIC_HEIGHT
    ) {
        "specific_height"
    } else {
        "auto"
    },
    playerQualityHeight = playerQualityHeight,
    subtitleOffsetX = subtitleOffsetX,
    subtitleOffsetY = subtitleOffsetY,
    subtitleTextSize = subtitleTextSize,
    subtitleTextColor = subtitleTextColor,
    subtitleBackgroundColor = subtitleBackgroundColor,
    subtitleBackgroundOpacity = subtitleBackgroundOpacity,
    updatedAt = updatedAt,
)

@Suppress("DEPRECATION")
private fun BackupTracking.toState() = EntryTrackingBackupRecord(
    serviceId = syncId.toLong(),
    remoteId = mediaIdInt.takeIf { it != 0 }?.toLong() ?: mediaId,
    libraryId = libraryId,
    title = title,
    progress = lastChapterRead.toDouble(),
    total = totalChapters.toLong(),
    score = score.toDouble(),
    status = status.toLong(),
    startDate = startedReadingDate,
    finishDate = finishedReadingDate,
    remoteUrl = trackingUrl,
    private = private,
)

private fun EntryTrackingBackupRecord.toWire() = BackupTracking(
    syncId = serviceId.toInt(),
    libraryId = libraryId ?: 0,
    mediaId = remoteId,
    trackingUrl = remoteUrl,
    title = title,
    lastChapterRead = progress.toFloat(),
    totalChapters = total.toInt(),
    score = score.toFloat(),
    status = status.toInt(),
    startedReadingDate = startDate,
    finishedReadingDate = finishDate,
    private = private,
)

private fun BackupDownloadPreferences.toState() = EntryDownloadConfigurationBackupState(
    dubKey = dubKey,
    streamKey = streamKey,
    subtitleKey = subtitleKey,
    qualityMode = when (qualityMode) {
        "best" -> EntryDownloadConfigurationQualityMode.BEST
        "data_saving" -> EntryDownloadConfigurationQualityMode.DATA_SAVING
        else -> EntryDownloadConfigurationQualityMode.BALANCED
    },
    updatedAt = updatedAt,
)

private fun EntryDownloadConfigurationBackupState.toWire() = BackupDownloadPreferences(
    dubKey = dubKey,
    streamKey = streamKey,
    subtitleKey = subtitleKey,
    qualityMode = when (qualityMode) {
        EntryDownloadConfigurationQualityMode.BEST -> "best"
        EntryDownloadConfigurationQualityMode.BALANCED -> "balanced"
        EntryDownloadConfigurationQualityMode.DATA_SAVING -> "data_saving"
    },
    updatedAt = updatedAt,
)
