package mihon.entry.interactions.navigation

import android.app.PendingIntent
import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class DefaultEntryOpenFeature(
    private val applicableTypes: Set<EntryType>,
    private val interaction: EntryOpenInteraction,
) : EntryOpenFeature {
    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override fun open(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
        options: EntryOpenOptions,
    ): Boolean {
        if (!isApplicable(entry.type)) return false
        interaction.open(context, entry, chapter, options)
        return true
    }

    override fun pendingIntent(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
        options: EntryOpenOptions,
    ): PendingIntent? {
        if (!isApplicable(entry.type)) return null
        return interaction.pendingIntent(context, entry, chapter, options)
    }
}
