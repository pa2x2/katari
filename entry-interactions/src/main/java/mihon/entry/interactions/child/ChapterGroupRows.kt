package mihon.entry.interactions.child

import tachiyomi.domain.entry.model.ChapterGroupId
import tachiyomi.domain.entry.model.EntryChapterGroup
import tachiyomi.domain.entry.model.groupId

/**
 * [this] rows, whose chapters come group by group within each member, with a header over each group. A member gets
 * headers only when its chapters span more than one group, counting the groups it declares without chapters yet; those
 * get a header of their own where their position puts them. A group whose chapters the filters hide all is left out.
 */
internal fun List<EntryChildListRow>.withChapterGroupHeaders(request: EntryChildListRequest): List<EntryChildListRow> {
    if (request.chapterGroups.isEmpty()) return this

    val descending = request.entry.sortDescending()
    val groupsByMember = request.chapterGroups.groupBy(EntryChapterGroup::entryId)
    val titles = request.chapterGroups.associate { ChapterGroupId(it.entryId, it.key) to it.title }
    val keysWithChapters = request.allChapters.groupBy { it.entryId }
        .mapValues { (_, chapters) -> chapters.mapTo(mutableSetOf()) { it.groupKey } }

    val rows = mutableListOf<EntryChildListRow>()
    var member: Long? = null
    var group: String? = null
    var headed = false
    val emptyGroups = ArrayDeque<EntryChapterGroup>()
    // A missing-chapter count before a group's first chapter goes under that group's header
    val missingCounts = mutableListOf<EntryChildListRow>()

    /** Heads the declared groups without chapters that come before [position] in display order, or all with null. */
    fun addEmptyGroups(position: Int?) {
        while (emptyGroups.isNotEmpty()) {
            val next = emptyGroups.first()
            if (position != null && (if (descending) next.position < position else next.position > position)) return
            emptyGroups.removeFirst()
            rows += EntryChildListRow.GroupHeader(next.entryId, next.key, next.title)
        }
    }

    fun startMember(memberId: Long?) {
        addEmptyGroups(position = null)
        member = memberId
        group = null
        val declared = groupsByMember[memberId].orEmpty()
        val withChapters = keysWithChapters[memberId].orEmpty()
        headed = (withChapters + declared.map(EntryChapterGroup::key)).size > 1
        if (headed) {
            val empty = declared.filter { it.key !in withChapters }
            emptyGroups += if (descending) empty.asReversed() else empty
        }
    }

    for (row in this) {
        when (row) {
            is EntryChildListRow.MemberHeader -> {
                startMember(null)
                rows += row
            }
            is EntryChildListRow.MissingCount -> missingCounts += row
            is EntryChildListRow.GroupHeader -> rows += row
            is EntryChildListRow.Child -> {
                val chapter = row.chapter
                val newMember = chapter.entryId != member
                if (newMember) startMember(chapter.entryId)
                if (headed && (newMember || chapter.groupKey != group)) {
                    // The chapters in no group have no position, so empty groups wait for the next group that has one
                    chapter.groupPosition?.let(::addEmptyGroups)
                    rows += EntryChildListRow.GroupHeader(chapter.entryId, chapter.groupKey, titles[chapter.groupId])
                }
                group = chapter.groupKey
                rows += missingCounts
                missingCounts.clear()
                rows += row
            }
        }
    }
    startMember(null)
    return rows
}
