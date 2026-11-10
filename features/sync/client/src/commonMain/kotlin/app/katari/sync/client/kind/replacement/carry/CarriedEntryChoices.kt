package app.katari.sync.client.kind.replacement.carry

import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.entrychoice.DownloadPreferencesKind
import app.katari.sync.client.kind.entrychoice.DownloadPreferencesPayload
import app.katari.sync.client.kind.entrychoice.EntryUpdateModeKind
import app.katari.sync.client.kind.entrychoice.EntryUpdateModePayload
import app.katari.sync.client.kind.entrychoice.ExcludedScanlatorKind
import app.katari.sync.client.kind.entrychoice.ExcludedScanlatorPayload
import app.katari.sync.client.kind.entrychoice.IntroTimesPickKind
import app.katari.sync.client.kind.entrychoice.IntroTimesPickPayload
import app.katari.sync.client.kind.entrychoice.PlaybackPreferencesKind
import app.katari.sync.client.kind.entrychoice.PlaybackPreferencesPayload
import app.katari.sync.client.kind.entrychoice.TheIntroDbPickKind
import app.katari.sync.client.kind.entrychoice.TheIntroDbPickPayload
import app.katari.sync.client.kind.entrychoice.TranslationLanguagesKind
import app.katari.sync.client.kind.entrychoice.TranslationLanguagesPayload
import app.katari.sync.client.kind.entrychoice.ViewerSettingKind
import app.katari.sync.client.kind.entrychoice.ViewerSettingPayload
import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.record.SyncedKind
import tachiyomi.domain.entry.migration.MigrationTranslationChoices

/**
 * The person's choices about an entry, carried over a replacement to the entry that replaced it. A migration carries
 * the ones local migration does: viewer settings, playback preferences and the translation choices
 * [MigrationTranslationChoices] names. The update mode, excluded scanlators, download preferences and intro picks
 * stay with the old entry, as they do when migrating here; a move, being the same entry, takes them all.
 */
internal class CarriedEntryChoices(private val carry: ReplacementCarry) {

    suspend fun choice(from: Any) {
        when (from) {
            is ViewerSettingPayload -> carried(ViewerSettingKind, from.providerId, from.settingKey) { to ->
                from.copy(entryKey = carry.to, encodedValue = pick(from.encodedValue, to?.encodedValue))
            }
            is PlaybackPreferencesPayload -> carried(PlaybackPreferencesKind) { to -> playbackPreferences(from, to) }
            is TranslationLanguagesPayload -> translationLanguages(from)
            is EntryUpdateModePayload -> if (carry.isMove) {
                carried(EntryUpdateModeKind) { to -> from.copy(entryKey = carry.to, mode = pick(from.mode, to?.mode)) }
            }
            is ExcludedScanlatorPayload -> if (carry.isMove) {
                carried(ExcludedScanlatorKind, from.scanlator) { to ->
                    from.copy(entryKey = carry.to, removed = pick(from.removed, to?.removed))
                }
            }
            is DownloadPreferencesPayload -> if (carry.isMove) {
                carried(DownloadPreferencesKind) { to -> downloadPreferences(from, to) }
            }
            is IntroTimesPickPayload -> if (carry.isMove) {
                carried(IntroTimesPickKind, from.groupKey) { to ->
                    from.copy(entryKey = carry.to, match = pick(from.match, to?.match))
                }
            }
            is TheIntroDbPickPayload -> if (carry.isMove) {
                carried(TheIntroDbPickKind, from.groupKey) { to ->
                    from.copy(entryKey = carry.to, match = pick(from.match, to?.match))
                }
            }
        }
    }

    /** A migration sets the new entry's choices as [MigrationTranslationChoices.appliedTo] does; a move takes them. */
    private suspend fun translationLanguages(from: TranslationLanguagesPayload) {
        carried(TranslationLanguagesKind) { to ->
            if (carry.isMove) {
                return@carried from.copy(
                    entryKey = carry.to,
                    contentLanguage = pick(from.contentLanguage, to?.contentLanguage),
                    targetLanguage = pick(from.targetLanguage, to?.targetLanguage),
                    translateDownloads = pick(from.translateDownloads, to?.translateDownloads),
                )
            }
            val targetLanguage: Stamped<String?> = to?.targetLanguage ?: Stamped(null, Stamped.UNKNOWN_EDIT)
            val translateDownloads = to?.translateDownloads ?: Stamped(false, Stamped.UNKNOWN_EDIT)
            val migrated = MigrationTranslationChoices(from.targetLanguage.value, from.translateDownloads.value)
                .appliedTo(MigrationTranslationChoices(targetLanguage.value, translateDownloads.value))
            TranslationLanguagesPayload(
                entryKey = carry.to,
                contentLanguage = to?.contentLanguage ?: Stamped(null, Stamped.UNKNOWN_EDIT),
                targetLanguage = ruled(migrated.targetLanguage, from.targetLanguage, targetLanguage),
                translateDownloads = ruled(migrated.translateDownloads, from.translateDownloads, translateDownloads),
            )
        }
    }

    private fun Picks.playbackPreferences(
        from: PlaybackPreferencesPayload,
        to: PlaybackPreferencesPayload?,
    ) = from.copy(
        entryKey = carry.to,
        dubKey = pick(from.dubKey, to?.dubKey),
        streamKey = pick(from.streamKey, to?.streamKey),
        sourceQualityKey = pick(from.sourceQualityKey, to?.sourceQualityKey),
        subtitleKey = pick(from.subtitleKey, to?.subtitleKey),
        playerQualityMode = pick(from.playerQualityMode, to?.playerQualityMode),
        playerQualityHeight = pick(from.playerQualityHeight, to?.playerQualityHeight),
        subtitleOffsetX = pick(from.subtitleOffsetX, to?.subtitleOffsetX),
        subtitleOffsetY = pick(from.subtitleOffsetY, to?.subtitleOffsetY),
        subtitleTextSize = pick(from.subtitleTextSize, to?.subtitleTextSize),
        subtitleTextColor = pick(from.subtitleTextColor, to?.subtitleTextColor),
        subtitleBackgroundColor = pick(from.subtitleBackgroundColor, to?.subtitleBackgroundColor),
        subtitleBackgroundOpacity = pick(from.subtitleBackgroundOpacity, to?.subtitleBackgroundOpacity),
        subtitleTextEdge = pick(from.subtitleTextEdge, to?.subtitleTextEdge),
        playerEngine = pick(from.playerEngine, to?.playerEngine),
        serverKey = pick(from.serverKey, to?.serverKey),
        subtitlePosition = pick(from.subtitlePosition, to?.subtitlePosition),
    )

    private fun Picks.downloadPreferences(
        from: DownloadPreferencesPayload,
        to: DownloadPreferencesPayload?,
    ) = from.copy(
        entryKey = carry.to,
        dubKey = pick(from.dubKey, to?.dubKey),
        streamKey = pick(from.streamKey, to?.streamKey),
        subtitleKey = pick(from.subtitleKey, to?.subtitleKey),
        qualityMode = pick(from.qualityMode, to?.qualityMode),
        serverKey = pick(from.serverKey, to?.serverKey),
        audioKey = pick(from.audioKey, to?.audioKey),
    )

    /**
     * Writes what [build] makes of the new entry's record of [kind], keyed by [keyParts], when it carries an edit made
     * after the replacement and changes the record. [build] returns null to leave it.
     */
    private suspend fun <P : Any> carried(kind: SyncedKind<P>, vararg keyParts: String, build: Picks.(to: P?) -> P?) {
        val key = RecordKeys.derive(kind.name, carry.to, *keyParts)
        val to = carry.stored(kind, key)
        val picks = Picks()
        val carried = picks.build(to) ?: return
        if (picks.edited && carried != to) carry.writes.change(kind, key, carried)
    }

    /**
     * [ReplacementCarry.pick] and [ReplacementCarry.ruled], noting whether any field took an edit made after the
     * replacement: a record without one has nothing to carry, whatever the new entry holds.
     */
    private inner class Picks {
        var edited = false
            private set

        fun <T> pick(source: Stamped<T>, target: Stamped<T>?): Stamped<T> {
            if (carry.after(source) != null) edited = true
            return carry.pick(source, target)
        }

        fun <T> ruled(value: T, source: Stamped<*>, target: Stamped<T>): Stamped<T> =
            carry.ruled(value, source, target).also { if (it !== target) edited = true }
    }
}
