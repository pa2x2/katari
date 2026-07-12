package tachiyomi.domain.entry.interactor

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.scale
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import eu.kanade.tachiyomi.util.system.getBitmapOrNull
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.DuplicateMatchReason
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.library.service.DuplicatePreferences
import kotlin.math.roundToInt

class EnhanceDuplicateLibraryEntries(
    private val entryRepository: EntryRepository,
    private val duplicatePreferences: DuplicatePreferences,
) {

    data class EnhancementRequest(
        val entryId: Long,
        val contentSignature: Long,
        val candidateSignatures: List<Pair<Long, Long>>,
    )

    suspend operator fun invoke(
        context: Context,
        entry: Entry,
        candidates: List<DuplicateEntryCandidate>,
        limit: Int = DEFAULT_CANDIDATE_LIMIT,
    ): List<DuplicateEntryCandidate> {
        val weightBudget = duplicatePreferences.getWeightBudget()
        if (!duplicatePreferences.extendedDuplicateDetectionEnabled.get()) return candidates
        if (weightBudget.cover <= 0) return candidates
        if (candidates.isEmpty() || entry.thumbnailUrl.isNullOrBlank()) return candidates

        val prioritized = candidates.sortedByDescending(DuplicateEntryCandidate::cheapScore)
        val topCandidates = prioritized.take(limit)
        val remainingCandidates = prioritized.drop(limit)

        val sourceHash = getOrComputeCoverHash(context, entry) ?: return prioritized

        val updatedTopCandidates = withIOContext {
            topCandidates.map { candidate ->
                async {
                    val candidateHash = getOrComputeCoverHash(context, candidate.entry)
                    if (candidateHash == null) {
                        candidate
                    } else {
                        val coverScore = coverScoreFromDistance(
                            distance = hammingDistance(sourceHash, candidateHash),
                            maxScore = weightBudget.cover,
                        )
                        val reasons = if (coverScore > 0 && DuplicateMatchReason.COVER !in candidate.reasons) {
                            candidate.reasons + DuplicateMatchReason.COVER
                        } else {
                            candidate.reasons
                        }
                        val newScoreMax = (candidate.scoreMax + weightBudget.cover).coerceAtLeast(1)
                        candidate.copy(
                            scoreMax = newScoreMax,
                            score = (candidate.cheapScore + coverScore)
                                .coerceIn(0, newScoreMax),
                            reasons = reasons,
                        )
                    }
                }
            }
                .awaitAll()
        }

        return (updatedTopCandidates + remainingCandidates)
            .sortedWith(
                compareByDescending<DuplicateEntryCandidate> { it.score }
                    .thenBy { it.entry.title.lowercase() },
            )
    }

    fun requestFor(entry: Entry, candidates: List<DuplicateEntryCandidate>): EnhancementRequest {
        return EnhancementRequest(
            entryId = entry.id,
            contentSignature = entry.lastModifiedAt,
            candidateSignatures = candidates.map { it.entry.id to it.contentSignature },
        )
    }

    private suspend fun getOrComputeCoverHash(context: Context, entry: Entry): Long? {
        if (entry.coverLastModified != 0L) {
            entryRepository.getCoverHash(entry.id, entry.coverLastModified)?.let { return it }
        }

        val bitmap = loadBitmap(context, entry) ?: return null
        val hash = bitmap.differenceHash()

        if (entry.coverLastModified != 0L) {
            entryRepository.upsertCoverHash(entry.id, entry.coverLastModified, hash)
        }

        return hash
    }

    private suspend fun loadBitmap(context: Context, entry: Entry): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(entry)
            .size(Size.ORIGINAL)
            .allowHardware(false)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
        val drawable = context.imageLoader.execute(request).image?.asDrawable(context.resources)
        return drawable?.getBitmapOrNull()
    }

    private fun Bitmap.differenceHash(): Long {
        val resized = scale(HASH_WIDTH, HASH_HEIGHT, true)
        var hash = 0L
        var bit = 0
        for (y in 0 until HASH_HEIGHT) {
            for (x in 0 until HASH_WIDTH - 1) {
                val left = resized.getPixel(x, y).grayscale()
                val right = resized.getPixel(x + 1, y).grayscale()
                if (left > right) {
                    hash = hash or (1L shl bit)
                }
                bit++
            }
        }
        if (resized !== this) {
            resized.recycle()
        }
        return hash
    }

    private fun Int.grayscale(): Int {
        val red = (this shr 16) and 0xFF
        val green = (this shr 8) and 0xFF
        val blue = this and 0xFF
        return ((red * 299) + (green * 587) + (blue * 114)) / 1000
    }

    private fun hammingDistance(left: Long, right: Long): Int {
        return java.lang.Long.bitCount(left xor right)
    }

    private fun coverScoreFromDistance(distance: Int, maxScore: Int): Int {
        if (maxScore <= 0) return 0
        return when {
            distance <= 4 -> maxScore
            distance <= 8 -> (maxScore * 0.75f).roundToInt()
            distance <= 12 -> (maxScore * 0.5f).roundToInt()
            distance <= 16 -> (maxScore * 0.25f).roundToInt()
            else -> 0
        }
    }

    private companion object {
        private const val DEFAULT_CANDIDATE_LIMIT = 12
        private const val HASH_WIDTH = 9
        private const val HASH_HEIGHT = 8
    }
}
