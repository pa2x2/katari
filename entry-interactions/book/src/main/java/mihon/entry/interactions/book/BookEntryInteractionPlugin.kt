package mihon.entry.interactions.book

import mihon.entry.interactions.EntryInteractionPlugin
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository

fun bookEntryInteractionPlugin(
    dependencies: BookEntryInteractionDependencies,
): EntryInteractionPlugin {
    return EntryInteractionPlugin { registry ->
        val openProcessor = BookOpenProcessor()
        registry.registerOpenProcessor(openProcessor)
        registry.registerCapabilityProcessor(BookCapabilityProcessor())
        registry.registerChildListProcessor(BookChildListProcessor(dependencies.entryProgressRepository))
        registry.registerContinueProcessor(
            BookContinueProcessor(
                getEntryWithChapters = dependencies.getEntryWithChapters,
                entryProgressRepository = dependencies.entryProgressRepository,
                openProcessor = openProcessor,
            ),
        )
        registry.registerConsumptionProcessor(
            BookConsumptionProcessor(
                entryProgressRepository = dependencies.entryProgressRepository,
                entryChapterRepository = dependencies.entryChapterRepository,
            ),
        )
        registry.registerUpdateEligibilityProcessor(BookUpdateEligibilityProcessor())
        registry.registerProgressProcessor(
            BookProgressProcessor(
                entryProgressRepository = dependencies.entryProgressRepository,
                entryChapterRepository = dependencies.entryChapterRepository,
            ),
        )
        registry.registerLibraryFilterProcessor(BookLibraryFilterProcessor())
    }
}

data class BookEntryInteractionDependencies(
    val getEntryWithChapters: GetEntryWithChapters,
    val entryChapterRepository: EntryChapterRepository,
    val entryProgressRepository: EntryProgressRepository,
)
