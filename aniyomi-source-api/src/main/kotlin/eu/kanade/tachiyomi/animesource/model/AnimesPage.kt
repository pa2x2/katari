package eu.kanade.tachiyomi.animesource.model

class AnimesPage(val animes: List<SAnime>, val hasNextPage: Boolean) {
    // AnimesPage was a data class up to extensions-lib 16
    operator fun component1(): List<SAnime> = animes

    operator fun component2(): Boolean = hasNextPage

    fun copy(animes: List<SAnime> = this.animes, hasNextPage: Boolean = this.hasNextPage): AnimesPage =
        AnimesPage(animes, hasNextPage)
}
