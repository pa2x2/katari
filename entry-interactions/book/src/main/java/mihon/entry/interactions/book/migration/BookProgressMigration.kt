package mihon.entry.interactions.book

import tachiyomi.domain.entry.model.EntryProgressState

internal const val BOOK_PENDING_MIGRATION_CONTENT_KEY = "app.katari.book.migration-pending"

internal fun bookPendingMigrationResourceKey(chapterId: Long): String = "target-child:$chapterId"

internal val EntryProgressState.isPendingBookMigration: Boolean
    get() = contentKey == BOOK_PENDING_MIGRATION_CONTENT_KEY
