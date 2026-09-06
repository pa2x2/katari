package eu.kanade.tachiyomi.source.entry.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import kotlin.test.Test
import kotlin.test.assertTrue

class EntryFilterIdentityTest {
    private class Choice(override val filterMetadata: EntryFilterMetadata) :
        EntryFilter.Select<String>("Choice", arrayOf("Any", "Movie")), EntryFilterMetadataProvider

    @Test
    fun `identity aliases cannot collide across different groups`() {
        val first = Choice(EntryFilterMetadata(id = "type", optionIds = listOf("any", "movie")))
        val second =
            Choice(EntryFilterMetadata(id = "other", aliases = setOf("type"), optionIds = listOf("any", "movie")))
        val nested = object : EntryFilter.Group<EntryFilter<*>>("Group", listOf(second)) {}
        assertTrue(EntryFilterList(first, nested).validationIssues().isNotEmpty())
    }

    @Test
    fun `opted in options must resolve uniquely including their neutral state and current selection`() {
        val choice =
            Choice(EntryFilterMetadata(id = "type", optionIds = listOf("any", "movie"), neutralOptionId = "any"))
        assertTrue(EntryFilterList(choice).validationIssues().isEmpty())
        choice.state = 2
        assertTrue(EntryFilterList(choice).validationIssues().isNotEmpty())
        val duplicated = Choice(EntryFilterMetadata(id = "type", optionIds = listOf("any", "any")))
        assertTrue(EntryFilterList(duplicated).validationIssues().isNotEmpty())
        val missingNeutral =
            Choice(EntryFilterMetadata(id = "type", optionIds = listOf("any", "movie"), neutralOptionId = "all"))
        assertTrue(EntryFilterList(missingNeutral).validationIssues().isNotEmpty())
    }
}
