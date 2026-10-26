package eu.kanade.domain.source.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import mihon.domain.extension.model.ContentWarning
import tachiyomi.core.common.preference.Preference

/**
 * The content warnings a profile allows, as one choice. Not rated is kept apart from the others, in [hideNotRated]:
 * [rated] was saved as a full set before Not rated existed, and a set saved then means nothing about it, so Not rated
 * stays allowed until a profile turns it off, in profiles chosen before as in new ones and in older backups.
 */
internal class AllowedContentWarnings(
    private val rated: Preference<Set<ContentWarning>>,
    private val hideNotRated: Preference<Boolean>,
) : Preference<Set<ContentWarning>> {

    override fun key(): String = rated.key()

    override fun get(): Set<ContentWarning> = combine(rated.get(), hideNotRated.get())

    override fun set(value: Set<ContentWarning>) {
        rated.set(value - ContentWarning.NOT_RATED)
        hideNotRated.set(ContentWarning.NOT_RATED !in value)
    }

    override fun isSet(): Boolean = rated.isSet() || hideNotRated.isSet()

    override fun delete() {
        rated.delete()
        hideNotRated.delete()
    }

    override fun defaultValue(): Set<ContentWarning> = combine(rated.defaultValue(), hideNotRated.defaultValue())

    override fun changes(): Flow<Set<ContentWarning>> = combine(rated.changes(), hideNotRated.changes(), ::combine)

    override fun stateIn(scope: CoroutineScope): StateFlow<Set<ContentWarning>> =
        changes().stateIn(scope, SharingStarted.Eagerly, get())

    private fun combine(rated: Set<ContentWarning>, hideNotRated: Boolean): Set<ContentWarning> {
        val others = rated - ContentWarning.NOT_RATED
        return if (hideNotRated) others else others + ContentWarning.NOT_RATED
    }
}
