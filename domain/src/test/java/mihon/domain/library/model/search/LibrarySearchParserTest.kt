package mihon.domain.library.model.search

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LibrarySearchParserTest {

    @Test
    fun `legacy syntax keeps commas as delimiters and id and source selectors as raw remainders`() {
        QueryNode.from("full metal, - villain") shouldBe AndNode(
            listOf(
                GeneralQueryNode("full metal", negated = false),
                GeneralQueryNode("villain", negated = true),
            ),
        )
        QueryNode.from("id:42,ignored") shouldBe ExactEntryIdQueryNode("42,ignored")
        QueryNode.from("src:123") shouldBe ExactSourceQueryNode("123")
    }

    @Test
    fun `explicit boolean syntax preserves precedence and grouping and unknown fields stay general text`() {
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
        QueryNode.from("unknown:value || title:known") shouldBe OrNode(
            listOf(
                GeneralQueryNode("unknown:value", negated = false),
                FieldQueryNode(EntryField.TITLE, "known", negated = false),
            ),
        )
    }
}
