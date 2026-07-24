package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.SpecializedAdapter
import mihon.feature.graph.SpecializedAdapterId
import mihon.feature.graph.specializedAdapterDefinition

/**
 * Type-owned participation in canonical child actions routed through [EntryWebViewFeature].
 *
 * The adapter has no dispatch method: application behavior must continue through the Feature result. It is contributed
 * beside the reader/player UI. Its presence makes the child WebView relationship eligible for that content type;
 * absence is ordinary unsupported behavior.
 */
interface EntryChildWebViewHostAdapter : EntryInteractionSpecializedAdapter {
    override val type: EntryType
}

object EntryChildWebViewHostContribution {
    val definition = specializedAdapterDefinition<EntryChildWebViewHostAdapter>(
        id = SpecializedAdapterId("entry.web-view.child-host"),
        owner = ContributionOwner("entry-web-view"),
    )

    fun bind(adapter: EntryChildWebViewHostAdapter): SpecializedAdapter<EntryChildWebViewHostAdapter> =
        SpecializedAdapter(definition, adapter)
}
