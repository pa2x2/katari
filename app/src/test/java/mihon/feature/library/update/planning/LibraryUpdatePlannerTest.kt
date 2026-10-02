package mihon.feature.library.update.planning

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import mihon.entry.interactions.state.EntryUpdateEligibility
import mihon.entry.interactions.state.EntryUpdateEligibilityFeature
import mihon.entry.interactions.state.EntryUpdateEligibilityRequest
import mihon.entry.interactions.state.EntryUpdateSkipReason
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryStatus
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.domain.library.update.model.CategoryUpdateOverrides
import tachiyomi.domain.library.update.model.CategoryUpdateRules
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason
import tachiyomi.domain.library.update.model.EntryUpdateMode
import tachiyomi.domain.library.update.model.LibraryUpdateSkipRules
import kotlin.time.Duration.Companion.hours

class LibraryUpdatePlannerTest {

    private val planner = LibraryUpdatePlanner(
        object : EntryUpdateEligibilityFeature {
            override fun evaluate(request: EntryUpdateEligibilityRequest): EntryUpdateEligibility {
                val skip = request.skipRules.skipCompleted && request.entry.status == EntryStatus.COMPLETED
                return if (skip) {
                    EntryUpdateEligibility.Skipped(
                        EntryUpdateSkipReason.COMPLETED,
                    )
                } else {
                    EntryUpdateEligibility.Eligible
                }
            }
        },
    )

    @Test
    fun `a stricter category skips entries its other categories check, after modes and switches decide`() {
        val settings = settings(
            categoryRules = listOf(
                CategoryUpdateRules(
                    STRICT,
                    overrides = CategoryUpdateOverrides(skipCompletedRules, intervalHours = null),
                ),
                CategoryUpdateRules(OFF, autoUpdate = false),
            ),
            entryModes = mapOf(4L to EntryUpdateMode.ALWAYS, 5L to EntryUpdateMode.NEVER),
        )
        val items = listOf(
            item(1, categories = listOf(STRICT, LENIENT), completed = true),
            item(2, categories = listOf(LENIENT), completed = true),
            item(3, categories = listOf(LENIENT, OFF)),
            item(4, categories = listOf(STRICT, OFF), completed = true),
            item(5, categories = listOf(LENIENT)),
        )

        val rules = planner.plan(LibraryUpdateRequest.FollowRules(automatic = false), items, settings, context())
        val selection = planner.plan(LibraryUpdateRequest.Selection(setOf(1, 3, 5)), items, settings, context())

        rules shouldBe listOf(
            LibraryUpdateDecision.Leave(items[0], EntryUpdateDecisionReason.COMPLETED, STRICT),
            LibraryUpdateDecision.Check(items[1]),
            LibraryUpdateDecision.Leave(items[2], EntryUpdateDecisionReason.CATEGORY_OFF, OFF),
            LibraryUpdateDecision.Check(items[3]),
            LibraryUpdateDecision.Leave(items[4], EntryUpdateDecisionReason.ENTRY_NEVER),
        )
        selection shouldBe listOf(
            LibraryUpdateDecision.Check(items[0]),
            LibraryUpdateDecision.Check(items[2]),
            LibraryUpdateDecision.Leave(items[4], EntryUpdateDecisionReason.ENTRY_NEVER),
        )
    }

    @Test
    fun `automatic updates wait out a slower category's interval, allowing half a run of drift`() {
        val settings = settings(
            categoryRules = listOf(
                CategoryUpdateRules(STRICT, overrides = CategoryUpdateOverrides(LibraryUpdateSkipRules.None, 168)),
            ),
        )
        val items = listOf(
            item(1, categories = listOf(STRICT)),
            item(2, categories = listOf(STRICT)),
            item(3, categories = listOf(STRICT)),
        )
        val lastCheckedAt = mapOf(
            1L to NOW - 48.hours.inWholeMilliseconds,
            2L to NOW - 160.hours.inWholeMilliseconds,
        )

        val automatic = planner.plan(
            LibraryUpdateRequest.FollowRules(automatic = true),
            items,
            settings,
            context(lastCheckedAt),
        )
        val manual = planner.plan(
            LibraryUpdateRequest.FollowRules(automatic = false),
            items,
            settings,
            context(lastCheckedAt),
        )

        automatic shouldBe listOf(
            LibraryUpdateDecision.Leave(items[0], EntryUpdateDecisionReason.NOT_DUE, STRICT),
            LibraryUpdateDecision.Check(items[1]),
            LibraryUpdateDecision.Check(items[2]),
        )
        manual shouldBe items.map { LibraryUpdateDecision.Check(it) }
    }

    private val skipCompletedRules = LibraryUpdateSkipRules.None.copy(skipCompleted = true)

    private fun settings(
        categoryRules: List<CategoryUpdateRules>,
        entryModes: Map<Long, EntryUpdateMode> = emptyMap(),
    ) = LibraryUpdateSettings(
        skipRules = LibraryUpdateSkipRules.None,
        intervalHours = 24,
        excludedSourceIds = emptySet(),
        excludedEntryTypes = emptySet(),
        categoryRules = categoryRules.associateBy(CategoryUpdateRules::categoryId),
        entryModes = entryModes,
    )

    private fun context(lastCheckedAt: Map<Long, Long> = emptyMap()) = LibraryUpdatePlanningContext(
        now = NOW,
        fetchWindowUpperBound = NOW,
        lastCheckedAt = lastCheckedAt,
    )

    private fun item(id: Long, categories: List<Long>, completed: Boolean = false): LibraryItem {
        val entry = Entry.create().copy(
            id = id,
            source = 1L,
            favorite = true,
            title = "Entry $id",
            type = EntryType.MANGA,
            status = if (completed) EntryStatus.COMPLETED else EntryStatus.ONGOING,
        )
        return LibraryItem(
            entry = entry,
            categories = categories,
            sourceName = "Source",
            sourceLanguage = "en",
            sourceItemOrientation = EntryItemOrientation.VERTICAL,
            displaySourceId = 1L,
            sourceIds = setOf(1L),
            isLocal = false,
            isMerged = false,
            memberEntryIds = listOf(LibraryItemKey(EntryType.MANGA, id)),
            memberEntries = listOf(entry),
            progressSummary = EntryLibraryProgressResolution.Inapplicable(EntryType.MANGA),
            latestUpload = 0L,
            downloadCount = 0,
        )
    }

    private companion object {
        const val LENIENT = 0L
        const val STRICT = 1L
        const val OFF = 2L
        const val NOW = 1_000_000_000_000L
    }
}
