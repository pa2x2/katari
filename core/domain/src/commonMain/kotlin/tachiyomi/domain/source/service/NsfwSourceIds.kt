package tachiyomi.domain.source.service

import kotlinx.coroutines.flow.Flow

/**
 * The sources that may have 18+ content: those of installed extensions not rated safe, including unrated ones. Views
 * such as the statistics recap leave their titles out unless the user opts in.
 */
fun interface NsfwSourceIds {
    fun subscribe(): Flow<Set<Long>>
}
