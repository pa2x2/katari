package app.katari.webpages.keeper

import app.katari.webpages.host.HostPages
import app.katari.webpages.page.DrivenWebPage
import eu.kanade.tachiyomi.source.entry.webpage.WebPage
import eu.kanade.tachiyomi.source.entry.webpage.WebPageException
import eu.kanade.tachiyomi.source.entry.webpage.WebPageSource
import eu.kanade.tachiyomi.source.entry.webpage.WebPages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import tachiyomi.core.common.preference.SourcePreferenceProfile
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The pages sources keep: one per source and name, which one caller has at a time and which stays open between calls,
 * so a site's app is loaded once and navigated after. A page left alone goes idle, then is closed; the next call makes
 * and prepares a new one, as it does after a page was lost, after the source's extension was replaced, and when the
 * call works for another profile than the page was prepared for, since what a source puts into its page may come from
 * a profile's settings.
 *
 * No more pages are open than [limits] allow. A page that would be one too many takes the place of the idle page used
 * longest ago, or waits for a page to be given back; a source's own page too many fails the call where none of its
 * pages is idle, as waiting for a page the source itself holds would never end.
 *
 * @param activeProfileId the profile a call works for unless its coroutine names another, as source settings resolve it.
 * @param scope where pages left alone are timed.
 */
class WebPageKeeper(
    private val hostPages: HostPages,
    private val limits: WebPageLimits,
    private val activeProfileId: () -> Long,
    private val scope: CoroutineScope,
) : WebPages {
    private val registry = Mutex()

    // Under the registry's lock
    private val slots = HashMap<SlotKey, Slot>()
    private var uses = 0L

    /** Changes whenever a page is given back or closed, which a call without room waits for. */
    private val room = MutableStateFlow(0L)

    override suspend fun <T> use(source: WebPageSource, name: String, block: suspend (page: WebPage) -> T): T {
        val slot = enter(source, name)
        try {
            return slot.turn.withLock {
                slot.idling?.cancel()
                val kept = slot.pageFor(source, name)
                try {
                    block(kept.page)
                } finally {
                    if (slot.kept != null) slot.idling = scope.launch { idle(slot) }
                }
            }
        } finally {
            withContext(NonCancellable) { leave(slot) }
        }
    }

    /**
     * The source's slot [name] with the caller counted in. A name that would be one too many for the source takes the
     * place of the source's idle page used longest ago.
     */
    private suspend fun enter(source: WebPageSource, name: String): Slot {
        var replaced: Kept? = null
        val slot = registry.withLock {
            val key = SlotKey(source.id, name)
            val slot = slots[key] ?: Slot(key).also { added ->
                val others = slots.values.filter { it.key.sourceId == source.id }
                if (others.size >= limits.perSource) {
                    val idle = others.filter { it.callers == 0 }.minByOrNull { it.lastUse }
                        ?: throw WebPageException(
                            "${source.name} uses more pages at once than the ${limits.perSource} Katari keeps " +
                                "for a source",
                        )
                    replaced = idle.kept
                    idle.kept = null
                    forget(idle)
                }
                slots[key] = added
            }
            slot.also { it.callers++ }
        }
        replaced?.let { page -> withContext(NonCancellable) { page.page.close() } }
        return slot
    }

    private suspend fun leave(slot: Slot) {
        registry.withLock {
            slot.callers--
            slot.lastUse = ++uses
            forget(slot)
        }
        room.update { it + 1 }
    }

    // Under the registry's lock
    private fun forget(slot: Slot) {
        if (slot.callers == 0 && slot.kept == null && !slot.isOpening) slots.remove(slot.key)
    }

    /** The slot's page where it still serves [source] in the caller's profile, else a new one in its place. */
    private suspend fun Slot.pageFor(source: WebPageSource, name: String): Kept {
        val profile = SourcePreferenceProfile.current ?: activeProfileId()
        kept?.let { held ->
            if (held.source === source && held.profileId == profile && !held.page.isLost) {
                if (held.isIdle) {
                    held.page.setIdle(false)
                    held.isIdle = false
                }
                return held
            }
            close(this)
        }
        reserveRoom(this)
        try {
            val spec = source.webPageSpec(name)
            val page = DrivenWebPage.open(hostPages, spec)
            try {
                page.load(spec.url)
                source.prepareWebPage(name, page)
            } catch (e: Throwable) {
                withContext(NonCancellable) { page.close() }
                throw e
            }
            return Kept(page, source, profile).also { opened ->
                registry.withLock {
                    kept = opened
                    isOpening = false
                }
            }
        } catch (e: Throwable) {
            withContext(NonCancellable) { registry.withLock { isOpening = false } }
            room.update { it + 1 }
            throw e
        }
    }

    /** Counts [slot] among the open pages once there is room for it, closing an idle page for it where there is none. */
    private suspend fun reserveRoom(slot: Slot) {
        withTimeoutOrNull(ROOM_TIMEOUT) {
            while (true) {
                val seen = room.value
                val replaced = registry.withLock {
                    val open = slots.values.count { it.kept != null || it.isOpening }
                    if (open < limits.total) {
                        slot.isOpening = true
                        return@withTimeoutOrNull
                    }
                    slots.values
                        .filter { it.callers == 0 && it.kept != null }
                        .minWithOrNull(compareBy<Slot>({ it.kept?.page?.isLost != true }, { it.lastUse }))
                        ?.let { idle ->
                            idle.kept.also {
                                idle.kept = null
                                forget(idle)
                            }
                        }
                }
                if (replaced != null) {
                    withContext(NonCancellable) { replaced.page.close() }
                } else {
                    room.first { it != seen }
                }
            }
        } ?: throw WebPageException("Every page Katari keeps for sources stayed busy for $ROOM_TIMEOUT")
    }

    private suspend fun idle(slot: Slot) {
        delay(IDLE_AFTER)
        // A caller that has the page by now cancels this as it starts; it isn't waited for
        if (!slot.turn.tryLock()) return
        try {
            slot.kept?.takeUnless { it.page.isLost }?.let { held ->
                held.page.setIdle(true)
                held.isIdle = true
            }
        } finally {
            slot.turn.unlock()
        }
        delay(CLOSE_AFTER - IDLE_AFTER)
        if (!slot.turn.tryLock()) return
        try {
            withContext(NonCancellable) { close(slot) }
        } finally {
            slot.turn.unlock()
        }
    }

    /** Closes the slot's page, by whoever has its turn. */
    private suspend fun close(slot: Slot) {
        val closed = registry.withLock {
            slot.kept.also {
                slot.kept = null
                forget(slot)
            }
        } ?: return
        closed.page.close()
        room.update { it + 1 }
    }

    private data class SlotKey(val sourceId: Long, val name: String)

    /**
     * A source's page by name, open or not.
     *
     * @property turn held by the caller that has the page, and by whatever idles or closes it.
     * @property callers how many calls have the page or wait for it, under the registry's lock as [kept], [isOpening]
     * and [lastUse] are.
     * @property isOpening whether a page is being made for it, which counts as open from then on.
     * @property idling times the page from when its last caller gave it back.
     */
    private class Slot(val key: SlotKey) {
        val turn = Mutex()
        var callers = 0
        var kept: Kept? = null
        var isOpening = false
        var lastUse = 0L
        var idling: Job? = null
    }

    /**
     * An open page.
     *
     * @property source the instance that prepared it, which an updated extension replaces.
     */
    private class Kept(val page: DrivenWebPage, val source: WebPageSource, val profileId: Long) {
        var isIdle = false
    }

    private companion object {
        val IDLE_AFTER = 10.seconds
        val CLOSE_AFTER = 5.minutes
        val ROOM_TIMEOUT = 30.seconds
    }
}
