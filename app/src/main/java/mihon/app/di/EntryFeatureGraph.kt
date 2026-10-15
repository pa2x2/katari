package mihon.app.di

import mihon.entry.interactions.statistics.EntryStatisticsFeature

/** The Entry Features the graph builds, as code outside the graph reads them. */
interface EntryFeatureGraph {
    val entryStatisticsFeature: EntryStatisticsFeature
}
