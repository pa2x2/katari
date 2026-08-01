package mihon.domain.library.model.search

sealed interface QueryNode {
    companion object {
        fun from(query: String): QueryNode {
            return LibrarySearchParser.parse(query)
        }
    }
}

data class AndNode(val children: List<QueryNode>) : QueryNode

data class OrNode(val children: List<QueryNode>) : QueryNode

data class NotNode(val child: QueryNode) : QueryNode

object EmptyQueryNode : QueryNode

data class GeneralQueryNode(val value: String, val negated: Boolean) : QueryNode

data class ExactEntryIdQueryNode(val value: String) : QueryNode

data class ExactSourceQueryNode(val value: String) : QueryNode

data class FieldQueryNode(val field: EntryField, val value: String, val negated: Boolean) : QueryNode

data class ComparisonQueryNode(
    val field: ComparisonField,
    val value: String,
    val queryComparator: Comparator,
    val negated: Boolean,
) : QueryNode
