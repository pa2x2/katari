package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.desktop.merge.NoEntryMergeConsequenceStatusFeature
import mihon.desktop.merge.NoMergeChildOwnership
import mihon.desktop.merge.NoMergeLibraryGrouping
import mihon.desktop.merge.NoMergeNavigation
import mihon.entry.interactions.merge.EntryMergeConsequenceStatusFeature
import mihon.entry.interactions.merge.EntryMergeNavigationFeature
import tachiyomi.domain.entry.service.EntryChildOwnershipResolutionPort
import tachiyomi.domain.entry.service.EntryLibraryGroupingResolutionPort

@BindingContainer
interface MergeBindings {
    @Binds
    val NoMergeLibraryGrouping.libraryGrouping: EntryLibraryGroupingResolutionPort

    @Binds
    val NoMergeChildOwnership.childOwnership: EntryChildOwnershipResolutionPort

    @Binds
    val NoEntryMergeConsequenceStatusFeature.entryMergeConsequenceStatusFeature: EntryMergeConsequenceStatusFeature

    @Binds
    val NoMergeNavigation.entryMergeNavigation: EntryMergeNavigationFeature
}
