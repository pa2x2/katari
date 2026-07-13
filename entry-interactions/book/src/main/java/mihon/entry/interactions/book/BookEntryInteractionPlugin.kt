package mihon.entry.interactions.book

import mihon.entry.interactions.EntryInteractionPlugin
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.source.service.SourceManager

fun bookEntryInteractionPlugin(
    dependencies: BookEntryInteractionDependencies,
): EntryInteractionPlugin {
    return EntryInteractionPlugin { registry ->
        val openProcessor = BookOpenProcessor()
        val identityResolver = BookProgressIdentityResolver(
            getEntryWithChapters = dependencies.getEntryWithChapters,
            sourceManager = dependencies.sourceManager,
        )
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
                identityResolver = identityResolver,
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
    val sourceManager: SourceManager,
)
