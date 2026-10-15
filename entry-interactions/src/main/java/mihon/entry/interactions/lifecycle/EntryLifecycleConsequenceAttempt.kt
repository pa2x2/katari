package mihon.entry.interactions.lifecycle

import kotlinx.coroutines.CancellationException

/** Runs one consequence of a change that has already committed, reporting its failure instead of undoing the change. */
internal suspend fun MutableList<EntryLifecycleConsequenceFailure>.attempt(
    consequence: String,
    block: suspend () -> Unit,
) {
    try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        add(EntryLifecycleConsequenceFailure(consequence, error))
    }
}
