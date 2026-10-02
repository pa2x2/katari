package tachiyomi.domain.library.update.model

data class CategoryUpdateRules(
    val categoryId: Long,
    val autoUpdate: Boolean = true,
    /** Replaces the library skip rules and interval for this category; null means it uses the library's. */
    val overrides: CategoryUpdateOverrides? = null,
) {
    val isDefault: Boolean
        get() = autoUpdate && overrides == null
}

data class CategoryUpdateOverrides(
    val skipRules: LibraryUpdateSkipRules,
    /** Hours between automatic checks; null keeps the library interval. */
    val intervalHours: Int?,
)
