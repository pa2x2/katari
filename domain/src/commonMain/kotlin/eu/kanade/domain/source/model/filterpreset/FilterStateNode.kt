package eu.kanade.domain.source.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = FilterStateNodeSerializer::class)
sealed interface FilterStateNode {
    val name: String
    val identity: FilterIdentity? get() = null

    @Serializable
    @SerialName("header")
    data class Header(
        override val name: String,
        override val identity: FilterIdentity? = null,
    ) : FilterStateNode

    @Serializable
    @SerialName("separator")
    data class Separator(
        override val name: String,
        override val identity: FilterIdentity? = null,
    ) : FilterStateNode

    @Serializable
    @SerialName("select")
    data class Select(
        override val name: String,
        val state: Int,
        override val identity: FilterIdentity? = null,
    ) : FilterStateNode

    @Serializable
    @SerialName("text")
    data class Text(
        override val name: String,
        val state: String,
        override val identity: FilterIdentity? = null,
    ) : FilterStateNode

    @Serializable
    @SerialName("checkbox")
    data class CheckBox(
        override val name: String,
        val state: Boolean,
        override val identity: FilterIdentity? = null,
    ) : FilterStateNode

    @Serializable
    @SerialName("tristate")
    data class TriState(
        override val name: String,
        val state: Int,
        override val identity: FilterIdentity? = null,
    ) : FilterStateNode

    @Serializable
    @SerialName("sort")
    data class Sort(
        override val name: String,
        val index: Int?,
        val ascending: Boolean?,
        override val identity: FilterIdentity? = null,
    ) : FilterStateNode

    @Serializable
    @SerialName("group")
    data class Group(
        override val name: String,
        val state: List<FilterStateNode>,
        override val identity: FilterIdentity? = null,
    ) : FilterStateNode

    @Serializable
    @SerialName("paged_group")
    data class PagedGroup(
        override val name: String,
        val state: String,
        override val identity: FilterIdentity? = null,
    ) : FilterStateNode
    data class Unknown(val raw: kotlinx.serialization.json.JsonElement) : FilterStateNode {
        override val name: String get() = (raw as? kotlinx.serialization.json.JsonObject)?.get("name")?.let {
            (it as? kotlinx.serialization.json.JsonPrimitive)?.content
        }
            ?: "Unknown filter"
    }
}

@Serializable
data class FilterIdentity(
    val id: String,
    val optionId: String? = null,
    val stateVersion: Int = 1,
)
