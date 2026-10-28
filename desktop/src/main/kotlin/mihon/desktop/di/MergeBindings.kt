package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.desktop.merge.NoEntryMergeConsequenceStatusFeature
import mihon.desktop.merge.NoEntryMergeFeature
import mihon.desktop.merge.NoMergeCandidates
import mihon.desktop.merge.NoMergeChildOwnership
import mihon.desktop.merge.NoMergeLibraryGrouping
import mihon.desktop.merge.NoMergeLibraryLifecycle
import mihon.desktop.merge.NoMergeMetadataRefresh
import mihon.desktop.merge.NoMergeNavigation
import mihon.entry.interactions.merge.EntryMergeCandidateFeature
import mihon.entry.interactions.merge.EntryMergeConsequenceStatusFeature
import mihon.entry.interactions.merge.EntryMergeFeature
import mihon.entry.interactions.merge.EntryMergeLibraryLifecycleFeature
import mihon.entry.interactions.merge.EntryMergeMetadataRefreshFeature
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

    @Binds
    val NoEntryMergeFeature.entryMergeFeature: EntryMergeFeature

    @Binds
    val NoMergeCandidates.entryMergeCandidateFeature: EntryMergeCandidateFeature

    @Binds
    val NoMergeLibraryLifecycle.entryMergeLibraryLifecycleFeature: EntryMergeLibraryLifecycleFeature

    @Binds
    val NoMergeMetadataRefresh.entryMergeMetadataRefreshFeature: EntryMergeMetadataRefreshFeature
}
