package tachiyomi.data.query

/**
 * Splits collection-valued SQL parameters below SQLite's legacy bind-parameter limit.
 *
 * This is a query chunk size, not a limit on the number of values supported by the caller.
 */
fun <T> Collection<T>.chunkedForSqlQuery(): List<List<T>> = chunked(SQL_QUERY_PARAMETER_CHUNK_SIZE)

private const val SQL_QUERY_PARAMETER_CHUNK_SIZE = 500
