package app.katari.sync.client.kind.entrychoice

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.apply.idOr
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.capture.CaptureTrigger.Event
import app.katari.sync.client.capture.CaptureTrigger.Field
import app.katari.sync.client.capture.CaptureTrigger.Owner
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.merge.latest
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.record.FieldStamps
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind

/**
 * A record of one entry's own choice, keyed by the entry's key and [keyParts]: found when the row is and the entry
 * syncs.
 */
private suspend fun <P : Any, R : Any> SyncedKind<P>.entryChoice(
    reads: LocalReads,
    entryId: Long,
    row: R?,
    vararg keyParts: String,
    build: (entryKey: String, row: R, previous: P?, stamps: FieldStamps) -> P,
): LocalRead<P> {
    row ?: return LocalRead.Missing
    val entryKey = reads.entryKey(entryId) ?: return LocalRead.NotSynced
    return LocalRead.Found(
        key = RecordKeys.derive(name, entryKey, *keyParts),
        parents = listOf(RecordRef(EntryKind, EntryKind.ref(entryId))),
    ) { previous, stamps -> build(entryKey, row, previous, stamps) }
}

/**
 * Inserts, updates of [fields] and deletes of [table] while the row's entry is here, for rows that meet [condition]
 * (of `NEW` or `OLD`). Inserts are
 * captured whatever [condition] says: a table written with `INSERT OR REPLACE` replaces a row that met it with one
 * that may not, and SQLite reports that as an insert alone.
 */
private fun triggers(
    table: String,
    ref: List<String>,
    fields: List<Field>,
    condition: ((row: String) -> String)? = null,
) = listOf(
    CaptureTrigger(table, Event.INSERT, ref.map { "NEW.$it" }),
    // A row that stops meeting the condition is captured too, so its record is deleted.
    CaptureTrigger(
        table,
        Event.UPDATE,
        ref.map { "NEW.$it" },
        fields = fields,
        condition = condition?.let { "${it("NEW")} OR ${it("OLD")}" },
    ),
    CaptureTrigger(
        table,
        Event.DELETE,
        ref.map { "OLD.$it" },
        condition = condition?.invoke("OLD"),
        owners = listOf(Owner.entry()),
    ),
)

/** Local ref: `[entry_id]`. */
object EntryUpdateModeKind : SyncedKind<EntryUpdateModePayload>(
    "entry.update-mode",
    1,
    EntryUpdateModePayload.serializer(),
    Absence.TOMBSTONE,
) {
    override val dataGroup = DataGroup.TITLE_CHOICES

    override val capture = triggers("entry_update_modes", listOf("entry_id"), listOf(Field(2, "mode")))

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<EntryUpdateModePayload> {
        val row = reads.database.sync_local_rowsQueries.entryUpdateModeRow(ref.long(0)).awaitAsOneOrNull()
        return entryChoice(reads, ref.long(0), row) { entryKey, row, previous, stamps ->
            EntryUpdateModePayload(entryKey, stamps.field(2, row.mode, previous?.mode))
        }
    }

    override fun merge(local: EntryUpdateModePayload, incoming: EntryUpdateModePayload) = local.copy(
        mode = latest(local.mode, incoming.mode),
    )

    override suspend fun apply(writes: LocalWrites, key: String, payload: EntryUpdateModePayload): ApplyOutcome {
        val entryId = writes.entryId(payload.entryKey).idOr { return it }
        writes.database.sync_applyQueries.upsertEntryUpdateMode(entryId, payload.mode.value)
        return ApplyOutcome.Applied(LocalRef.of(entryId))
    }

    override suspend fun applyDeletion(writes: LocalWrites, key: String, ref: LocalRef?): ApplyOutcome {
        ref?.let { writes.database.sync_applyQueries.deleteEntryUpdateMode(it.long(0)) }
        return ApplyOutcome.Applied(null)
    }

    override fun entryKeyOf(payload: EntryUpdateModePayload) = payload.entryKey
}

/** Local ref: `[entry_id, scanlator]`. */
object ExcludedScanlatorKind : SyncedKind<ExcludedScanlatorPayload>(
    "entry.excluded-scanlator",
    1,
    ExcludedScanlatorPayload.serializer(),
    Absence.REMOVED_FLAG,
) {
    override val dataGroup = DataGroup.TITLE_CHOICES

    override val capture = listOf(
        CaptureTrigger("excluded_scanlators", Event.INSERT, listOf("NEW.entry_id", "NEW.scanlator")),
        CaptureTrigger(
            "excluded_scanlators",
            Event.DELETE,
            listOf("OLD.entry_id", "OLD.scanlator"),
            owners = listOf(Owner.entry()),
        ),
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<ExcludedScanlatorPayload> {
        val row = reads.database.sync_local_rowsQueries.excludedScanlatorRow(ref.long(0), ref.text(1))
            .awaitAsOneOrNull()
        return entryChoice(reads, ref.long(0), row, ref.text(1)) { entryKey, row, previous, stamps ->
            ExcludedScanlatorPayload(entryKey, row.scanlator, stamps.field(3, false, previous?.removed))
        }
    }

    override fun removed(previous: ExcludedScanlatorPayload, stamps: FieldStamps) =
        previous.copy(removed = stamps.field(3, true, previous.removed))

    override fun merge(local: ExcludedScanlatorPayload, incoming: ExcludedScanlatorPayload) = local.copy(
        removed = latest(local.removed, incoming.removed),
    )

    override suspend fun apply(writes: LocalWrites, key: String, payload: ExcludedScanlatorPayload): ApplyOutcome {
        val entryId = writes.entryId(payload.entryKey).idOr { return it }
        val queries = writes.database.sync_applyQueries
        val excluded = queries.excludedScanlatorExists(entryId, payload.scanlator).awaitAsOne()
        when {
            payload.removed.value -> queries.deleteExcludedScanlator(entryId, payload.scanlator)
            !excluded -> queries.insertExcludedScanlator(
                queries.entryProfile(entryId).awaitAsOne(),
                entryId,
                payload.scanlator,
            )
        }
        return ApplyOutcome.Applied(LocalRef.of(entryId, payload.scanlator))
    }

    override fun entryKeyOf(payload: ExcludedScanlatorPayload) = payload.entryKey
}

/** Local ref: `[entry_id, provider_id, setting_key]`. */
object ViewerSettingKind : SyncedKind<ViewerSettingPayload>(
    "entry.viewer-setting",
    1,
    ViewerSettingPayload.serializer(),
    Absence.TOMBSTONE,
) {
    override val dataGroup = DataGroup.TITLE_CHOICES

    override val capture = triggers(
        "viewer_setting_overrides",
        listOf("entry_id", "provider_id", "setting_key"),
        listOf(Field(4, "encoded_value")),
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<ViewerSettingPayload> {
        val row = reads.database.sync_local_rowsQueries
            .viewerSettingOverrideRow(ref.long(0), ref.text(1), ref.text(2))
            .awaitAsOneOrNull()
        return entryChoice(reads, ref.long(0), row, ref.text(1), ref.text(2)) { entryKey, row, previous, stamps ->
            ViewerSettingPayload(
                entryKey = entryKey,
                providerId = row.provider_id,
                settingKey = row.setting_key,
                encodedValue = stamps.field(4, row.encoded_value, previous?.encodedValue),
            )
        }
    }

    override fun merge(local: ViewerSettingPayload, incoming: ViewerSettingPayload) = local.copy(
        encodedValue = latest(local.encodedValue, incoming.encodedValue),
    )

    override suspend fun apply(writes: LocalWrites, key: String, payload: ViewerSettingPayload): ApplyOutcome {
        val entryId = writes.entryId(payload.entryKey).idOr { return it }
        writes.database.sync_applyQueries.upsertViewerSetting(
            entryId = entryId,
            providerId = payload.providerId,
            settingKey = payload.settingKey,
            encodedValue = payload.encodedValue.value,
        )
        return ApplyOutcome.Applied(LocalRef.of(entryId, payload.providerId, payload.settingKey))
    }

    override suspend fun applyDeletion(writes: LocalWrites, key: String, ref: LocalRef?): ApplyOutcome {
        ref?.let { writes.database.sync_applyQueries.deleteViewerSetting(it.long(0), it.text(1), it.text(2)) }
        return ApplyOutcome.Applied(null)
    }

    override fun entryKeyOf(payload: ViewerSettingPayload) = payload.entryKey
}

/** Local ref: `[entry_id]`. */
object DownloadPreferencesKind : SyncedKind<DownloadPreferencesPayload>(
    "entry.download-preferences",
    1,
    DownloadPreferencesPayload.serializer(),
    Absence.TOMBSTONE,
) {
    override val dataGroup = DataGroup.TITLE_CHOICES

    override val capture = triggers(
        "download_preferences",
        listOf("entry_id"),
        listOf(
            Field(2, "dub_key"),
            Field(3, "stream_key"),
            Field(4, "subtitle_key"),
            Field(5, "quality_mode"),
            Field(6, "server_key"),
            Field(7, "audio_key"),
        ),
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<DownloadPreferencesPayload> {
        val row = reads.database.sync_local_rowsQueries.downloadPreferencesRow(ref.long(0)).awaitAsOneOrNull()
        return entryChoice(reads, ref.long(0), row) { entryKey, row, previous, stamps ->
            DownloadPreferencesPayload(
                entryKey = entryKey,
                dubKey = stamps.field(2, row.dub_key, previous?.dubKey),
                streamKey = stamps.field(3, row.stream_key, previous?.streamKey),
                subtitleKey = stamps.field(4, row.subtitle_key, previous?.subtitleKey),
                qualityMode = stamps.field(5, row.quality_mode, previous?.qualityMode),
                serverKey = stamps.field(6, row.server_key, previous?.serverKey),
                audioKey = stamps.field(7, row.audio_key, previous?.audioKey),
            )
        }
    }

    override fun merge(local: DownloadPreferencesPayload, incoming: DownloadPreferencesPayload) = local.copy(
        dubKey = latest(local.dubKey, incoming.dubKey),
        streamKey = latest(local.streamKey, incoming.streamKey),
        subtitleKey = latest(local.subtitleKey, incoming.subtitleKey),
        qualityMode = latest(local.qualityMode, incoming.qualityMode),
        serverKey = latest(local.serverKey, incoming.serverKey),
        audioKey = latest(local.audioKey, incoming.audioKey),
    )

    override suspend fun apply(writes: LocalWrites, key: String, payload: DownloadPreferencesPayload): ApplyOutcome {
        val entryId = writes.entryId(payload.entryKey).idOr { return it }
        writes.database.sync_applyQueries.upsertDownloadPreferences(
            entryId = entryId,
            dubKey = payload.dubKey.value,
            streamKey = payload.streamKey.value,
            subtitleKey = payload.subtitleKey.value,
            qualityMode = payload.qualityMode.value,
            serverKey = payload.serverKey.value,
            audioKey = payload.audioKey.value,
        )
        return ApplyOutcome.Applied(LocalRef.of(entryId))
    }

    override suspend fun applyDeletion(writes: LocalWrites, key: String, ref: LocalRef?): ApplyOutcome {
        ref?.let { writes.database.sync_applyQueries.deleteDownloadPreferences(it.long(0)) }
        return ApplyOutcome.Applied(null)
    }

    override fun entryKeyOf(payload: DownloadPreferencesPayload) = payload.entryKey
}

/** Local ref: `[entry_id]`. */
object TranslationLanguagesKind : SyncedKind<TranslationLanguagesPayload>(
    "entry.translation-languages",
    1,
    TranslationLanguagesPayload.serializer(),
    Absence.TOMBSTONE,
) {
    override val dataGroup = DataGroup.TITLE_CHOICES

    override val capture = triggers(
        "entry_translation_languages",
        listOf("entry_id"),
        listOf(Field(2, "content_language"), Field(3, "target_language"), Field(4, "translate_downloads")),
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<TranslationLanguagesPayload> {
        val row = reads.database.sync_local_rowsQueries.translationLanguagesRow(ref.long(0)).awaitAsOneOrNull()
        return entryChoice(reads, ref.long(0), row) { entryKey, row, previous, stamps ->
            TranslationLanguagesPayload(
                entryKey = entryKey,
                contentLanguage = stamps.field(2, row.content_language, previous?.contentLanguage),
                targetLanguage = stamps.field(3, row.target_language, previous?.targetLanguage),
                translateDownloads = stamps.field(4, row.translate_downloads, previous?.translateDownloads),
            )
        }
    }

    override fun merge(local: TranslationLanguagesPayload, incoming: TranslationLanguagesPayload) = local.copy(
        contentLanguage = latest(local.contentLanguage, incoming.contentLanguage),
        targetLanguage = latest(local.targetLanguage, incoming.targetLanguage),
        translateDownloads = latest(local.translateDownloads, incoming.translateDownloads),
    )

    override suspend fun apply(writes: LocalWrites, key: String, payload: TranslationLanguagesPayload): ApplyOutcome {
        val entryId = writes.entryId(payload.entryKey).idOr { return it }
        writes.database.sync_applyQueries.upsertTranslationLanguages(
            entryId = entryId,
            contentLanguage = payload.contentLanguage.value,
            targetLanguage = payload.targetLanguage.value,
            translateDownloads = payload.translateDownloads.value,
        )
        return ApplyOutcome.Applied(LocalRef.of(entryId))
    }

    override suspend fun applyDeletion(writes: LocalWrites, key: String, ref: LocalRef?): ApplyOutcome {
        ref?.let { writes.database.sync_applyQueries.deleteTranslationLanguages(it.long(0)) }
        return ApplyOutcome.Applied(null)
    }

    override fun entryKeyOf(payload: TranslationLanguagesPayload) = payload.entryKey
}

/**
 * Local ref: `[entry_id, group_key]`. Automatic matches are each device's own, so only rows the person picked are
 * captured, and a pick replaced by an automatic match deletes the record.
 */
object IntroTimesPickKind : SyncedKind<IntroTimesPickPayload>(
    "entry.intro-times-pick",
    1,
    IntroTimesPickPayload.serializer(),
    Absence.TOMBSTONE,
) {
    override val dataGroup = DataGroup.TITLE_CHOICES

    override val capture = triggers(
        "intro_times_matches",
        listOf("entry_id", "group_key"),
        listOf(Field(3, "anilist_id", "mal_id", "title", "year", "picked_by_user")),
        condition = { "$it.picked_by_user = 1" },
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<IntroTimesPickPayload> {
        val row = reads.database.sync_local_rowsQueries.introTimesPickRow(ref.long(0), ref.text(1)).awaitAsOneOrNull()
        return entryChoice(reads, ref.long(0), row, ref.text(1)) { entryKey, row, previous, stamps ->
            IntroTimesPickPayload(
                entryKey = entryKey,
                groupKey = row.group_key,
                match = stamps.field(
                    3,
                    IntroTimesMatch(row.anilist_id, row.mal_id, row.title, row.year),
                    previous?.match,
                ),
            )
        }
    }

    override fun merge(local: IntroTimesPickPayload, incoming: IntroTimesPickPayload) = local.copy(
        match = latest(local.match, incoming.match),
    )

    override suspend fun apply(writes: LocalWrites, key: String, payload: IntroTimesPickPayload): ApplyOutcome {
        val entryId = writes.entryId(payload.entryKey).idOr { return it }
        val match = payload.match.value
        writes.database.sync_applyQueries.upsertIntroTimesPick(
            entryId = entryId,
            groupKey = payload.groupKey,
            anilistId = match.anilistId,
            malId = match.malId,
            title = match.title,
            year = match.year,
        )
        return ApplyOutcome.Applied(LocalRef.of(entryId, payload.groupKey))
    }

    override suspend fun applyDeletion(writes: LocalWrites, key: String, ref: LocalRef?): ApplyOutcome {
        ref?.let { writes.database.sync_applyQueries.deleteIntroTimesPick(it.long(0), it.text(1)) }
        return ApplyOutcome.Applied(null)
    }

    override fun entryKeyOf(payload: IntroTimesPickPayload) = payload.entryKey
}

/** Local ref: `[entry_id, group_key]`. Synced like [IntroTimesPickKind]: only the rows the person picked. */
object TheIntroDbPickKind : SyncedKind<TheIntroDbPickPayload>(
    "entry.theintrodb-pick",
    1,
    TheIntroDbPickPayload.serializer(),
    Absence.TOMBSTONE,
) {
    override val dataGroup = DataGroup.TITLE_CHOICES

    override val capture = triggers(
        "theintrodb_matches",
        listOf("entry_id", "group_key"),
        listOf(
            Field(
                3,
                "tvdb_id",
                "tmdb_id",
                "imdb_id",
                "title",
                "year",
                "season",
                "first_episode",
                "picked_by_user",
            ),
        ),
        condition = { "$it.picked_by_user = 1" },
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<TheIntroDbPickPayload> {
        val row = reads.database.sync_local_rowsQueries.theIntroDbPickRow(ref.long(0), ref.text(1)).awaitAsOneOrNull()
        return entryChoice(reads, ref.long(0), row, ref.text(1)) { entryKey, row, previous, stamps ->
            TheIntroDbPickPayload(
                entryKey = entryKey,
                groupKey = row.group_key,
                match = stamps.field(
                    3,
                    TheIntroDbPick(
                        tvdbId = row.tvdb_id,
                        tmdbId = row.tmdb_id,
                        imdbId = row.imdb_id,
                        title = row.title,
                        year = row.year,
                        season = row.season,
                        firstEpisode = row.first_episode,
                    ),
                    previous?.match,
                ),
            )
        }
    }

    override fun merge(local: TheIntroDbPickPayload, incoming: TheIntroDbPickPayload) = local.copy(
        match = latest(local.match, incoming.match),
    )

    override suspend fun apply(writes: LocalWrites, key: String, payload: TheIntroDbPickPayload): ApplyOutcome {
        val entryId = writes.entryId(payload.entryKey).idOr { return it }
        val match = payload.match.value
        writes.database.sync_applyQueries.upsertTheIntroDbPick(
            entryId = entryId,
            groupKey = payload.groupKey,
            tvdbId = match.tvdbId,
            tmdbId = match.tmdbId,
            imdbId = match.imdbId,
            title = match.title,
            year = match.year,
            season = match.season,
            firstEpisode = match.firstEpisode,
        )
        return ApplyOutcome.Applied(LocalRef.of(entryId, payload.groupKey))
    }

    override suspend fun applyDeletion(writes: LocalWrites, key: String, ref: LocalRef?): ApplyOutcome {
        ref?.let { writes.database.sync_applyQueries.deleteTheIntroDbPick(it.long(0), it.text(1)) }
        return ApplyOutcome.Applied(null)
    }

    override fun entryKeyOf(payload: TheIntroDbPickPayload) = payload.entryKey
}

/** Local ref: `[entry_id]`. */
object PlaybackPreferencesKind : SyncedKind<PlaybackPreferencesPayload>(
    "entry.playback-preferences",
    1,
    PlaybackPreferencesPayload.serializer(),
    Absence.TOMBSTONE,
) {
    override val dataGroup = DataGroup.TITLE_CHOICES

    override val capture = triggers(
        "playback_preferences",
        listOf("entry_id"),
        listOf(
            Field(2, "dub_key"),
            Field(3, "stream_key"),
            Field(4, "source_quality_key"),
            Field(5, "subtitle_key"),
            Field(6, "player_quality_mode"),
            Field(7, "player_quality_height"),
            Field(8, "subtitle_offset_x"),
            Field(9, "subtitle_offset_y"),
            Field(10, "subtitle_text_size"),
            Field(11, "subtitle_text_color"),
            Field(12, "subtitle_background_color"),
            Field(13, "subtitle_background_opacity"),
            Field(14, "subtitle_text_edge"),
            Field(15, "player_engine"),
            Field(16, "server_key"),
        ),
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<PlaybackPreferencesPayload> {
        val row = reads.database.sync_local_rowsQueries.playbackPreferencesRow(ref.long(0)).awaitAsOneOrNull()
        return entryChoice(reads, ref.long(0), row) { entryKey, row, previous, stamps ->
            PlaybackPreferencesPayload(
                entryKey = entryKey,
                dubKey = stamps.field(2, row.dub_key, previous?.dubKey),
                streamKey = stamps.field(3, row.stream_key, previous?.streamKey),
                sourceQualityKey = stamps.field(4, row.source_quality_key, previous?.sourceQualityKey),
                subtitleKey = stamps.field(5, row.subtitle_key, previous?.subtitleKey),
                playerQualityMode = stamps.field(6, row.player_quality_mode, previous?.playerQualityMode),
                playerQualityHeight = stamps.field(7, row.player_quality_height, previous?.playerQualityHeight),
                subtitleOffsetX = stamps.field(8, row.subtitle_offset_x, previous?.subtitleOffsetX),
                subtitleOffsetY = stamps.field(9, row.subtitle_offset_y, previous?.subtitleOffsetY),
                subtitleTextSize = stamps.field(10, row.subtitle_text_size, previous?.subtitleTextSize),
                subtitleTextColor = stamps.field(11, row.subtitle_text_color, previous?.subtitleTextColor),
                subtitleBackgroundColor = stamps.field(
                    12,
                    row.subtitle_background_color,
                    previous?.subtitleBackgroundColor,
                ),
                subtitleBackgroundOpacity = stamps.field(
                    13,
                    row.subtitle_background_opacity,
                    previous?.subtitleBackgroundOpacity,
                ),
                subtitleTextEdge = stamps.field(14, row.subtitle_text_edge, previous?.subtitleTextEdge),
                playerEngine = stamps.field(15, row.player_engine, previous?.playerEngine),
                serverKey = stamps.field(16, row.server_key, previous?.serverKey),
            )
        }
    }

    override fun merge(local: PlaybackPreferencesPayload, incoming: PlaybackPreferencesPayload) = local.copy(
        dubKey = latest(local.dubKey, incoming.dubKey),
        streamKey = latest(local.streamKey, incoming.streamKey),
        sourceQualityKey = latest(local.sourceQualityKey, incoming.sourceQualityKey),
        subtitleKey = latest(local.subtitleKey, incoming.subtitleKey),
        playerQualityMode = latest(local.playerQualityMode, incoming.playerQualityMode),
        playerQualityHeight = latest(local.playerQualityHeight, incoming.playerQualityHeight),
        subtitleOffsetX = latest(local.subtitleOffsetX, incoming.subtitleOffsetX),
        subtitleOffsetY = latest(local.subtitleOffsetY, incoming.subtitleOffsetY),
        subtitleTextSize = latest(local.subtitleTextSize, incoming.subtitleTextSize),
        subtitleTextColor = latest(local.subtitleTextColor, incoming.subtitleTextColor),
        subtitleBackgroundColor = latest(local.subtitleBackgroundColor, incoming.subtitleBackgroundColor),
        subtitleBackgroundOpacity = latest(local.subtitleBackgroundOpacity, incoming.subtitleBackgroundOpacity),
        subtitleTextEdge = latest(local.subtitleTextEdge, incoming.subtitleTextEdge),
        playerEngine = latest(local.playerEngine, incoming.playerEngine),
        serverKey = latest(local.serverKey, incoming.serverKey),
    )

    override suspend fun apply(writes: LocalWrites, key: String, payload: PlaybackPreferencesPayload): ApplyOutcome {
        val entryId = writes.entryId(payload.entryKey).idOr { return it }
        writes.database.sync_applyQueries.upsertPlaybackPreferences(
            entryId = entryId,
            dubKey = payload.dubKey.value,
            streamKey = payload.streamKey.value,
            sourceQualityKey = payload.sourceQualityKey.value,
            subtitleKey = payload.subtitleKey.value,
            playerQualityMode = payload.playerQualityMode.value,
            playerQualityHeight = payload.playerQualityHeight.value,
            subtitleOffsetX = payload.subtitleOffsetX.value,
            subtitleOffsetY = payload.subtitleOffsetY.value,
            subtitleTextSize = payload.subtitleTextSize.value,
            subtitleTextColor = payload.subtitleTextColor.value,
            subtitleBackgroundColor = payload.subtitleBackgroundColor.value,
            subtitleBackgroundOpacity = payload.subtitleBackgroundOpacity.value,
            subtitleTextEdge = payload.subtitleTextEdge.value,
            playerEngine = payload.playerEngine.value,
            serverKey = payload.serverKey.value,
        )
        return ApplyOutcome.Applied(LocalRef.of(entryId))
    }

    override suspend fun applyDeletion(writes: LocalWrites, key: String, ref: LocalRef?): ApplyOutcome {
        ref?.let { writes.database.sync_applyQueries.deletePlaybackPreferences(it.long(0)) }
        return ApplyOutcome.Applied(null)
    }

    override fun entryKeyOf(payload: PlaybackPreferencesPayload) = payload.entryKey
}
