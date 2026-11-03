package mihon.entry.interactions.library

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.navigation.EntryContinueBatchSeed
import mihon.entry.interactions.navigation.EntryContinueFeature
import mihon.entry.interactions.navigation.EntryContinueTargetResult
import mihon.entry.interactions.navigation.SeededEntryContinueBatchFeature
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.service.EntryLibraryContinueTarget
import tachiyomi.domain.entry.service.EntryLibraryProgressMember
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.entry.service.EntryLibraryProgressSummary

internal class DefaultEntryLibraryProgressFeature(
    private val types: EntryLibraryProgressTypes,
    private val interaction: EntryLibraryProgressInteraction,
    private val continueFeature: EntryContinueFeature,
    private val entryProgressRepository: EntryProgressRepository,
) : EntryLibraryProgressFeature {
    override fun isApplicable(type: EntryType): Boolean = type in types.applicable

    override suspend fun calculate(
        entry: Entry,
        chapters: List<EntryChapter>,
        lastRead: Long,
    ): EntryLibraryProgressResolution {
        if (!isApplicable(entry.type)) return EntryLibraryProgressResolution.Inapplicable(entry.type)
        val evidence = interaction.evidence(entry, chapters)
        val continueTarget = if (entry.type in types.continueTarget) {
            continueFeature.nextTarget(entry).toLibraryTarget(entry.type)
        } else {
            EntryLibraryContinueTarget.Inapplicable
        }
        return buildResolution(entry, chapters, lastRead, evidence, continueTarget)
    }

    override suspend fun calculateBatch(
        members: List<EntryLibraryProgressMember>,
    ): Map<Long, EntryLibraryProgressResolution> {
        val progressStates = entryProgressRepository
            .getByEntryIds(members.mapTo(mutableSetOf()) { it.entry.id })
        val progressByEntryId = progressStates
            .groupBy(EntryProgressState::entryId)
        val entries = members.map(EntryLibraryProgressMember::entry)
        val continueTargets = if (continueFeature is SeededEntryContinueBatchFeature) {
            continueFeature.nextTargets(
                entries = entries,
                seed = EntryContinueBatchSeed(
                    completeOwnerIds = entries.mapTo(mutableSetOf(), Entry::id),
                    chapters = members.flatMap(EntryLibraryProgressMember::chapters),
                    progressStates = progressStates,
                ),
            )
        } else {
            continueFeature.nextTargets(entries)
        }

        return buildMap {
            members.forEach { member ->
                val entry = member.entry
                if (!isApplicable(entry.type)) {
                    put(entry.id, EntryLibraryProgressResolution.Inapplicable(entry.type))
                } else {
                    val evidence = interaction.evidence(
                        entry = entry,
                        chapters = member.chapters,
                        progressStates = progressByEntryId[entry.id].orEmpty(),
                    )
                    val continueTarget = if (entry.type in types.continueTarget) {
                        (continueTargets[entry.id] ?: continueFeature.nextTarget(entry))
                            .toLibraryTarget(entry.type)
                    } else {
                        EntryLibraryContinueTarget.Inapplicable
                    }
                    put(
                        entry.id,
                        buildResolution(entry, member.chapters, member.lastRead, evidence, continueTarget),
                    )
                }
            }
        }
    }

    private fun buildResolution(
        entry: Entry,
        chapters: List<EntryChapter>,
        lastRead: Long,
        evidence: EntryLibraryProgressEvidence,
        continueTarget: EntryLibraryContinueTarget,
    ): EntryLibraryProgressResolution {
        return EntryLibraryProgressResolution.Available(
            EntryLibraryProgressSummary(
                totalCount = chapters.size.toLong(),
                consumedCount = chapters.count(EntryChapter::read).toLong(),
                hasStarted = chapters.any(EntryChapter::read) || evidence.hasMediaProgress,
                bookmarkCount = chapters.count(EntryChapter::bookmark).toLong()
                    .takeIf { entry.type in types.bookmark },
                inProgressItemId = evidence.inProgressItemId,
                inProgressFraction = evidence.inProgressFraction,
                lastRead = maxOf(lastRead, evidence.lastActivityAt),
                continueTarget = continueTarget,
            ),
        )
    }

    private fun EntryContinueTargetResult.toLibraryTarget(entryType: EntryType): EntryLibraryContinueTarget {
        return when (this) {
            is EntryContinueTargetResult.Available -> EntryLibraryContinueTarget.Available(chapter.id)
            EntryContinueTargetResult.NoNext -> EntryLibraryContinueTarget.NoNext
            EntryContinueTargetResult.Inapplicable -> error(
                "Library progress selected Continue for $entryType, but Continue returned inapplicable",
            )
        }
    }

    override fun merge(
        entryType: EntryType,
        members: List<EntryLibraryProgressSummary>,
    ): EntryLibraryProgressResolution {
        if (!isApplicable(entryType)) return EntryLibraryProgressResolution.Inapplicable(entryType)
        require(members.isNotEmpty()) { "Cannot merge an empty Library progress group" }

        val inProgress = members.firstOrNull { it.inProgressItemId != null }
        val continueTarget = when {
            entryType !in types.continueTarget -> EntryLibraryContinueTarget.Inapplicable
            else -> members.asSequence()
                .map(EntryLibraryProgressSummary::continueTarget)
                .filterIsInstance<EntryLibraryContinueTarget.Available>()
                .firstOrNull()
                ?: EntryLibraryContinueTarget.NoNext
        }
        val bookmarkCount = if (entryType in types.bookmark) {
            members.sumOf { member ->
                checkNotNull(member.bookmarkCount) {
                    "Library progress Bookmark behavior selected $entryType without bookmark summary evidence"
                }
            }
        } else {
            null
        }

        return EntryLibraryProgressResolution.Available(
            EntryLibraryProgressSummary(
                totalCount = members.sumOf(EntryLibraryProgressSummary::totalCount),
                consumedCount = members.sumOf(EntryLibraryProgressSummary::consumedCount),
                hasStarted = members.any(EntryLibraryProgressSummary::hasStarted),
                bookmarkCount = bookmarkCount,
                inProgressItemId = inProgress?.inProgressItemId,
                inProgressFraction = inProgress?.inProgressFraction,
                lastRead = members.maxOf(EntryLibraryProgressSummary::lastRead),
                continueTarget = continueTarget,
            ),
        )
    }
}

/** The types each Library progress capability applies to, read from the providers the type modules contribute. */
internal class EntryLibraryProgressTypes(
    val applicable: Set<EntryType>,
    val continueTarget: Set<EntryType>,
    val bookmark: Set<EntryType>,
)
