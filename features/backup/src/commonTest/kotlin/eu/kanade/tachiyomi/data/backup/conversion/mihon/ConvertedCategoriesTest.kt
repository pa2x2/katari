package eu.kanade.tachiyomi.data.backup.conversion.mihon

import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Name
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.i18n.*

class ConvertedCategoriesTest {

    @Test
    fun `an order two categories share puts its entries in both and the report names them`() {
        // As TachiyomiSY wrote it after restoring into a library that had categories
        val categories = ConvertedCategories(
            listOf(
                MihonCategory("Reading", order = 0, id = 3),
                MihonCategory("Action", order = 1, id = 1),
                MihonCategory("Plan to read", order = 1, id = 4),
                MihonCategory("Completed", order = 5, id = 5),
            ),
        )

        val nameOf = categories.backupCategories.associate { it.order to it.name }
        nameOf.size shouldBe 4
        categories.of(0, listOf(1, 5)).map(nameOf::getValue) shouldBe listOf("Action", "Plan to read", "Completed")
        categories.reportItems.filter { it.note == MR.strings.backup_conversion_category_shared_order }
            .map { it.name } shouldBe listOf(Name.Text("Action"), Name.Text("Plan to read"))
    }

    @Test
    fun `a later list's category id in settings names the same category once ids are merged`() {
        // Aniyomi's anime categories number their ids apart from its manga ones
        val categories = ConvertedCategories(
            listOf(MihonCategory("Reading", order = 0, id = 1), MihonCategory("Favorites", order = 1, id = 2)),
            listOf(MihonCategory("Favorites", order = 0, id = 1), MihonCategory("Watching", order = 1, id = 2)),
        )

        val nameOf = categories.backupCategories.associate { it.id to it.name }
        categories.idOf(1, 1)?.let(nameOf::get) shouldBe "Favorites"
        categories.idOf(1, 2)?.let(nameOf::get) shouldBe "Watching"
        categories.idOf(1, 9) shouldBe null
    }
}
