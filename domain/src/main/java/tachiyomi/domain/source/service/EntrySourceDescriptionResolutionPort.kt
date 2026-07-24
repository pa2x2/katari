package tachiyomi.domain.source.service

import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.source.model.EntrySourceDescription

/**
 * Domain-side port for source description consumers.
 *
 * The application-facing implementation is owned by the Entry Catalogue Feature. Keeping this neutral port in Domain
 * avoids reversing the existing `entry-interactions:api -> domain` dependency.
 */
fun interface EntrySourceDescriptionResolutionPort {
    fun describe(source: UnifiedSource): EntrySourceDescription
}
