package mihon.entry.interactions

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryOpenFeatureTest {
    private val context = mockk<Context>(relaxed = true)
    private val chapter = EntryChapter.create().copy(id = 12L)

    @Test
    fun `missing Open provider is valid and exposes no Open action`() {
        val composition = createEntryInteractionComposition(
            plugins = emptyList(),
            featureContributors = listOf(EntryOpenFeatureContributor),
        )
        val feature = DefaultEntryOpenFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.open,
        )
        val entry = Entry.create().copy(id = 7L, type = EntryType.BOOK)

        feature.isApplicable(entry.type).shouldBeFalse()
        feature.open(context, entry, chapter).shouldBeFalse()
        feature.pendingIntent(context, entry, chapter).shouldBeNull()
    }
}
