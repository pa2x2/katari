package mihon.domain.library.model.search

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LibrarySearchParserTest {

    @Test
    fun `legacy commas delimit constraints while spaces remain part of a constraint`() {
        QueryNode.from("full metal, - villain") shouldBe AndNode(
            listOf(
                GeneralQueryNode("full metal", negated = false),
                GeneralQueryNode("villain", negated = true),
            ),
        )
    }

    @Test
    fun `legacy id and source selectors consume the complete remaining query`() {
        QueryNode.from("id:42,ignored") shouldBe ExactEntryIdQueryNode("42,ignored")
        QueryNode.from("src:123") shouldBe ExactSourceQueryNode("123")
    }

    @Test
    fun `explicit boolean syntax preserves comparison precedence and grouping`() {
        QueryNode.from("title:\"Full Metal\" || (author:Arakawa && unread>=2)") shouldBe OrNode(
            listOf(
                FieldQueryNode(EntryField.TITLE, "Full Metal", negated = false),
                AndNode(
                    listOf(
                        FieldQueryNode(EntryField.AUTHOR, "Arakawa", negated = false),
                        ComparisonQueryNode(ComparisonField.UNREAD, "2", Comparator.GTE, negated = false),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `unknown fields remain searchable as general text`() {
        QueryNode.from("unknown:value || title:known") shouldBe OrNode(
            listOf(
                GeneralQueryNode("unknown:value", negated = false),
                FieldQueryNode(EntryField.TITLE, "known", negated = false),
            ),
        )
    }
}
