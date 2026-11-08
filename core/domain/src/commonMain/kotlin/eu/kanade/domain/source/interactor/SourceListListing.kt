package eu.kanade.domain.source.interactor

sealed interface SourceListListing {
    data object Popular : SourceListListing
    data object Latest : SourceListListing
}
