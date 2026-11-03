package mihon.tools.extensioncompatibility.report

import mihon.tools.extensioncompatibility.results.ExtensionResult
import mihon.tools.extensioncompatibility.results.Outcome
import mihon.tools.extensioncompatibility.results.OutcomeKind

/** A class or member desktop lacks or couldn't convert, and the extensions that ran into it. */
internal data class MissingMember(
    val member: String,
    val kind: OutcomeKind,
    val extensions: List<ExtensionResult>,
)

/** Outcomes naming what desktop lacks or couldn't convert, the gaps the report and its diff track. */
private val GAP_KINDS = setOf(OutcomeKind.PLATFORM_GAP, OutcomeKind.NEEDS_WEB_VIEW, OutcomeKind.CONVERSION_FAILED)

/** Every outcome of the extension: its own, its sources' and their calls'. */
internal fun ExtensionResult.allOutcomes(): List<Outcome> =
    listOf(outcome) + sources.flatMap { source -> listOf(source.outcome) + source.calls.map { it.outcome } }

/** The members the extension or any of its sources ran into, with the outcome kind each came with. */
internal fun ExtensionResult.missingMembers(): Map<String, OutcomeKind> = allOutcomes()
    .filter { it.kind in GAP_KINDS }
    .mapNotNull { outcome -> outcome.member?.let { it to outcome.kind } }
    .toMap()

/** Each missing member with the extensions that ran into it, the most widespread first. */
internal fun List<ExtensionResult>.byMissingMember(): List<MissingMember> =
    flatMap { extension -> extension.missingMembers().map { (member, kind) -> Triple(member, kind, extension) } }
        .groupBy({ (member, kind, _) -> member to kind }, { (_, _, extension) -> extension })
        .map { (key, extensions) -> MissingMember(key.first, key.second, extensions.distinctBy { it.key }) }
        .sortedWith(compareByDescending<MissingMember> { it.extensions.size }.thenBy { it.member })
