package mihon.entry.interactions.child

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class DefaultEntryChildListFeature(
    private val types: EntryChildListTypes,
    private val childList: EntryChildListInteraction,
    private val childProgress: EntryChildProgressInteraction,
    private val missingChildGap: EntryMissingChildGapInteraction,
) : EntryChildListFeature {
    override fun isApplicable(type: EntryType): Boolean = type in types.childList

    override fun readingOrder(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): EntryChildOrderResult {
        if (entry.type !in types.childList) return EntryChildOrderResult.Inapplicable(entry.type)
        return EntryChildOrderResult.Available(childList.sortedForReading(entry, chapters, memberIds))
    }

    override fun displayOrder(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): EntryChildOrderResult {
        if (entry.type !in types.childList) return EntryChildOrderResult.Inapplicable(entry.type)
        return EntryChildOrderResult.Available(childList.sortedForDisplay(entry, chapters, memberIds))
    }

    override fun firstReadingChild(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): EntryFirstChildResult {
        if (entry.type !in types.childList) return EntryFirstChildResult.Inapplicable(entry.type)
        return EntryFirstChildResult.Available(
            childList.sortedForReading(entry, chapters, memberIds).firstOrNull(),
        )
    }

    override fun displayList(request: EntryChildListRequest): EntryChildListResult {
        if (request.entry.type !in types.childList) return EntryChildListResult.Inapplicable(request.entry.type)
        val display = if (request.entry.type in types.missingGap) {
            missingChildGap.buildDisplayList(request)
        } else {
            buildDefaultDisplayList(request)
        }
        return EntryChildListResult.Available(display)
    }

    override fun progressLabels(request: EntryChildProgressRequest): EntryChildProgressResult {
        if (request.entry.type !in types.progress) return EntryChildProgressResult.Inapplicable(request.entry.type)
        return EntryChildProgressResult.Available(childProgress.progressLabels(request))
    }

    private fun buildDefaultDisplayList(request: EntryChildListRequest): EntryChildListDisplay {
        val sortedChildren = childList.sortedForDisplay(request.entry, request.chapters, request.memberIds)
        val rows: List<EntryChildListRow> = if (request.memberIds.size <= 1) {
            sortedChildren.map(EntryChildListRow::Child)
        } else {
            buildList<EntryChildListRow> {
                request.memberIds.forEach { memberId ->
                    val memberChildren = sortedChildren.filter { it.entryId == memberId }
                    if (memberChildren.isEmpty()) return@forEach
                    add(
                        EntryChildListRow.MemberHeader(
                            entryId = memberId,
                            title = request.memberTitleById[memberId].orEmpty().ifBlank { request.fallbackTitle },
                        ),
                    )
                    addAll(memberChildren.map(EntryChildListRow::Child))
                }
            }
        }
        return EntryChildListDisplay(rows = rows, aggregateMissingCount = 0)
    }
}

/** The types each Child List capability applies to, read from the providers the type modules contribute. */
internal class EntryChildListTypes(
    val childList: Set<EntryType>,
    val progress: Set<EntryType>,
    val missingGap: Set<EntryType>,
)
