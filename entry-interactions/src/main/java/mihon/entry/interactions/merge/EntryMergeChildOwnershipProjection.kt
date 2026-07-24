package mihon.entry.interactions

import tachiyomi.domain.entry.service.EntryChildOwnershipResolutionPort

/** Internal Merge projection consumed by child-owning feature coordinators, never by application screens. */
internal interface EntryMergeChildOwnershipProjection : EntryChildOwnershipResolutionPort
