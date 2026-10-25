package mihon.entry.interactions.anime.download.staging

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ResumeJournalTest {

    @Test
    fun `only files recorded whole under the current plan are kept`() {
        val journal = ResumeJournal.planLine("video.m3u", "plan-a") +
            ResumeJournal.doneLine("video.m3u", "video-v-1.ts", 1000) +
            ResumeJournal.planLine("audio_1.m3u", "plan-x") +
            ResumeJournal.doneLine("audio_1.m3u", "audio_1-v-1.aac", 300) +
            ResumeJournal.doneLine("video.m3u", "video-v-2.ts", 2000) +
            // The process died while writing this record: its size is cut short.
            ResumeJournal.doneLine("video.m3u", "video-v-3.ts", 3000).dropLast(3)

        ResumeJournal.completed(journal, "video.m3u", "plan-a") shouldBe
            mapOf("video-v-1.ts" to 1000L, "video-v-2.ts" to 2000L)
        ResumeJournal.completed(journal, "video.m3u", "plan-b") shouldBe null

        // A plan that changed and changed back may have rewritten the same names in between.
        val replanned = journal.substringBeforeLast('\n') + "\n" +
            ResumeJournal.planLine("video.m3u", "plan-b") +
            ResumeJournal.doneLine("video.m3u", "video-v-1.ts", 900) +
            ResumeJournal.planLine("video.m3u", "plan-a")
        ResumeJournal.completed(replanned, "video.m3u", "plan-a") shouldBe emptyMap()
    }
}
