package tachiyomi.core.common.preference

import kotlinx.coroutines.asContextElement
import kotlin.coroutines.CoroutineContext

/**
 * The profile whose source preferences, where extensions keep logins and API keys, source calls in a coroutine read.
 *
 * Extensions read their preferences without saying for which profile, so they get the active one. Work that keeps
 * running for its own profile after another is opened, such as a library update or a download, runs its source calls
 * in [of] that profile instead. It follows the coroutine onto every thread it resumes on, but not onto threads the
 * coroutine hands work to, such as OkHttp's for an enqueued call.
 */
object SourcePreferenceProfile {
    private val profileId = ThreadLocal<Long?>()

    /** The profile set for the running coroutine; null outside such work, where the active profile applies. */
    val current: Long?
        get() = profileId.get()

    fun of(profileId: Long): CoroutineContext.Element = this.profileId.asContextElement(profileId)
}
