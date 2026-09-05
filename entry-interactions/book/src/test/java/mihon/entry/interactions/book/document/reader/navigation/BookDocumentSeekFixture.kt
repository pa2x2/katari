package mihon.entry.interactions.book.document.reader.navigation

import mihon.entry.interactions.book.document.preparation.preparedDocumentPublication
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import tachiyomi.domain.entry.model.EntryChapter

internal fun seekSection(resource: String = "text", chapterId: Long = 1) =
    preparedDocumentPublication(
        resource to "<p id='first'>${"Opening paragraph. ".repeat(20)}</p>" +
            "<p id='second'>${"Later passage. ".repeat(20)}</p>",
    ).documents.single().let { document ->
        BookDocumentSection(
            key = "$chapterId:$resource",
            owner = EntryChapter.create().copy(id = chapterId, name = "Chapter $chapterId"),
            document = PreparedBookDocument(document),
            initialPosition = document.positionAtProgression(0f),
            resourceLoader = null,
        )
    }
