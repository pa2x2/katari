package mihon.feature.profiles.core

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: Long,
    val uuid: String,
    val name: String,
    val colorSeed: Long,
    val position: Long,
    val requiresAuth: Boolean,
    val isArchived: Boolean,
)
