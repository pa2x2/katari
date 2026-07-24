package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.EntryLibraryContinueTarget
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.entry.service.EntryLibraryProgressSummary

internal class DefaultEntryLibraryProgressFeature(
    evaluation: FeatureGraphEvaluation,
    private val interaction: EntryLibraryProgressInteraction,
    private val continueFeature: EntryContinueFeature,
) : EntryLibraryProgressFeature {
    private val selection = evaluation.libraryProgressSelection()

    override fun isApplicable(type: EntryType): Boolean = type in selection.applicableTypes

    override suspend fun calculate(
        entry: Entry,
        chapters: List<EntryChapter>,
        lastRead: Long,
    ): EntryLibraryProgressResolution {
        if (!isApplicable(entry.type)) return EntryLibraryProgressResolution.Inapplicable(entry.type)
        val evidence = interaction.evidence(entry, chapters)
        val continueTarget = if (entry.type in selection.continueTypes) {
            when (val target = continueFeature.nextTarget(entry)) {
                is EntryContinueTargetResult.Available -> EntryLibraryContinueTarget.Available(target.chapter.id)
                EntryContinueTargetResult.NoNext -> EntryLibraryContinueTarget.NoNext
                EntryContinueTargetResult.Inapplicable -> error(
                    "Library progress selected Continue for ${entry.type}, but Continue returned inapplicable",
                )
            }
        } else {
            EntryLibraryContinueTarget.Inapplicable
        }
        return EntryLibraryProgressResolution.Available(
            EntryLibraryProgressSummary(
                totalCount = chapters.size.toLong(),
                consumedCount = chapters.count(EntryChapter::read).toLong(),
                hasStarted = chapters.any(EntryChapter::read) || evidence.hasMediaProgress,
                bookmarkCount = chapters.count(EntryChapter::bookmark).toLong()
                    .takeIf { entry.type in selection.bookmarkTypes },
                inProgressItemId = evidence.inProgressItemId,
                inProgressFraction = evidence.inProgressFraction,
                lastRead = maxOf(lastRead, evidence.lastActivityAt),
                continueTarget = continueTarget,
            ),
        )
    }

    override fun merge(
        entryType: EntryType,
        members: List<EntryLibraryProgressSummary>,
    ): EntryLibraryProgressResolution {
        if (!isApplicable(entryType)) return EntryLibraryProgressResolution.Inapplicable(entryType)
        require(members.isNotEmpty()) { "Cannot merge an empty Library progress group" }

        val inProgress = members.firstOrNull { it.inProgressItemId != null }
        val continueTarget = when {
            entryType !in selection.continueTypes -> EntryLibraryContinueTarget.Inapplicable
            else -> members.asSequence()
                .map(EntryLibraryProgressSummary::continueTarget)
                .filterIsInstance<EntryLibraryContinueTarget.Available>()
                .firstOrNull()
                ?: EntryLibraryContinueTarget.NoNext
        }
        val bookmarkCount = if (entryType in selection.bookmarkTypes) {
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
