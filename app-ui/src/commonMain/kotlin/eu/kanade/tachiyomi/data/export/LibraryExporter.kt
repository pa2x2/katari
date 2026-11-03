package eu.kanade.tachiyomi.data.export

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.storage.DocumentUri

object LibraryExporter {

    data class ExportOptions(
        val includeTitle: Boolean,
        val includeAuthor: Boolean,
        val includeArtist: Boolean,
    )

    suspend fun exportToCsv(
        documents: LibraryExportDocuments,
        uri: DocumentUri,
        favorites: List<Entry>,
        options: ExportOptions,
        onExportComplete: () -> Unit,
    ) {
        withContext(Dispatchers.IO) {
            documents.write(uri, generateCsvData(favorites, options))
            onExportComplete()
        }
    }

    private val escapeRequired = listOf("\r", "\n", "\"", ",")

    private fun generateCsvData(favorites: List<Entry>, options: ExportOptions): String {
        val columnSize = listOf(
            options.includeTitle,
            options.includeAuthor,
            options.includeArtist,
        )
            .count { it }

        val rows = buildList(favorites.size) {
            favorites.forEach { manga ->
                buildList(columnSize) {
                    if (options.includeTitle) add(manga.title)
                    if (options.includeAuthor) add(manga.author)
                    if (options.includeArtist) add(manga.artist)
                }
                    .let(::add)
            }
        }
        return rows.joinToString("\r\n") { columns ->
            columns.joinToString(",") columns@{ column ->
                if (column.isNullOrBlank()) return@columns ""
                if (escapeRequired.any { column.contains(it) }) {
                    column.replace("\"", "\"\"").let { "\"$it\"" }
                } else {
                    column
                }
            }
        }
    }
}
