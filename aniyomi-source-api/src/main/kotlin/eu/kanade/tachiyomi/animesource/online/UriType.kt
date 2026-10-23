package eu.kanade.tachiyomi.animesource.online

sealed interface UriType {
    data object Anime : UriType
    data object Episode : UriType
    data object Unknown : UriType
}
