package mihon.extension.smoke.report

import mihon.extension.smoke.results.ExtensionResult
import mihon.extension.smoke.results.SmokeResults

/**
 * What changed since an earlier run, over the extensions both runs covered, so a run of a few extensions doesn't count
 * every other one as gone: which work now and didn't, which stopped working, which fail differently, and which missing
 * members appeared or disappeared.
 */
internal data class ReportDiff(
    val previousReport: String,
    val compared: Int,
    val onlyNow: Int,
    val onlyBefore: Int,
    val newlyWorking: List<Change>,
    val newlyFailing: List<Change>,
    val failingDifferently: List<Change>,
    val gapsAppeared: List<MissingMember>,
    val gapsDisappeared: List<MissingMember>,
) {
    data class Change(val before: ExtensionResult, val after: ExtensionResult)

    companion object {
        fun between(previousReport: String, before: SmokeResults, after: SmokeResults): ReportDiff {
            val earlier = before.extensions.associateBy { it.key }
            val now = after.extensions.associateBy { it.key }
            val changes = now.values.mapNotNull { current -> earlier[current.key]?.let { Change(it, current) } }
            val comparedBefore = changes.map { it.before }.byMissingMember().associateBy { it.member }
            val comparedNow = changes.map { it.after }.byMissingMember().associateBy { it.member }
            return ReportDiff(
                previousReport = previousReport,
                compared = changes.size,
                onlyNow = now.keys.count { it !in earlier },
                onlyBefore = earlier.keys.count { it !in now },
                newlyWorking = changes.filter { !it.before.outcome.isWorking && it.after.outcome.isWorking },
                newlyFailing = changes.filter { it.before.outcome.isWorking && !it.after.outcome.isWorking },
                failingDifferently = changes.filter {
                    !it.before.outcome.isWorking && !it.after.outcome.isWorking &&
                        it.before.outcome.kind != it.after.outcome.kind
                },
                gapsAppeared = comparedNow.values.filter { it.member !in comparedBefore },
                gapsDisappeared = comparedBefore.values.filter { it.member !in comparedNow },
            )
        }
    }
}
