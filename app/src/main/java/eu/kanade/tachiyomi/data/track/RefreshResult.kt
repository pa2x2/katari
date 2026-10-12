package eu.kanade.tachiyomi.data.track

sealed interface RefreshResult {
    data object Success : RefreshResult
    data class Error(val msg: String) : RefreshResult
}
