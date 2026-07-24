package mihon.entry.interactions

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class DefaultEntryDownloadOptionsFeature(
    evaluation: FeatureGraphEvaluation,
    private val interaction: EntryDownloadInteraction,
) : EntryDownloadOptionsFeature {
    private val applicableTypes = evaluation.applicableProviderTypes<EntryDownloadOptionsProcessor>(
        feature = ENTRY_DOWNLOAD_CONFIGURATION_FEATURE_ID,
        integration = ENTRY_DOWNLOAD_OPTIONS_INTEGRATION_ID,
        behaviorProjection = ENTRY_DOWNLOAD_OPTIONS_BEHAVIOR_ID,
    )

    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override suspend fun resolve(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
    ): EntryDownloadOptionsResolution {
        if (!isApplicable(entry.type)) return EntryDownloadOptionsResolution.Inapplicable
        return interaction.resolveDownloadOptions(context, entry, chapter)
            ?.let(EntryDownloadOptionsResolution::Resolved)
            ?: EntryDownloadOptionsResolution.ContextuallyUnavailable
    }

    override suspend fun download(
        entry: Entry,
        chapters: List<EntryChapter>,
        selection: EntryDownloadOptionSelection,
        startNow: Boolean,
    ): EntryDownloadOptionsActionResult {
        if (!isApplicable(entry.type)) return EntryDownloadOptionsActionResult.Inapplicable
        interaction.downloadWithOptions(entry, chapters, selection, startNow)
        return EntryDownloadOptionsActionResult.Performed
    }
}
