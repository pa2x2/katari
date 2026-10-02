package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.update.model.CategoryUpdateOverrides
import tachiyomi.domain.library.update.model.CategoryUpdateRules
import tachiyomi.domain.library.update.model.LibraryUpdateSkipRules

@Serializable
class BackupCategory(
    @ProtoNumber(1) var name: String,
    @ProtoNumber(2) var order: Long = 0,
    @ProtoNumber(3) var id: Long = 0,
    // @ProtoNumber(3) val updateInterval: Int = 0, 1.x value not used in 0.x
    @ProtoNumber(100) var flags: Long = 0,
    /** Null when the category uses the default update rules. */
    @ProtoNumber(101) var updateRules: BackupCategoryUpdateRules? = null,
) {
    fun toCategory(id: Long) = Category(
        id = id,
        name = this@BackupCategory.name,
        flags = this@BackupCategory.flags,
        order = this@BackupCategory.order,
    )
}

@Serializable
class BackupCategoryUpdateRules(
    @ProtoNumber(1) var autoUpdate: Boolean = true,
    @ProtoNumber(2) var customRules: Boolean = false,
    @ProtoNumber(3) var skipCompleted: Boolean = false,
    @ProtoNumber(4) var skipUnseen: Boolean = false,
    @ProtoNumber(5) var skipNotStarted: Boolean = false,
    @ProtoNumber(6) var skipOutsideReleasePeriod: Boolean = false,
    @ProtoNumber(7) var intervalHours: Int? = null,
) {
    fun toCategoryUpdateRules(categoryId: Long) = CategoryUpdateRules(
        categoryId = categoryId,
        autoUpdate = autoUpdate,
        overrides = if (customRules) {
            CategoryUpdateOverrides(
                skipRules = LibraryUpdateSkipRules(
                    skipCompleted = skipCompleted,
                    skipUnseen = skipUnseen,
                    skipNotStarted = skipNotStarted,
                    skipOutsideReleasePeriod = skipOutsideReleasePeriod,
                ),
                intervalHours = intervalHours,
            )
        } else {
            null
        },
    )
}

fun CategoryUpdateRules.toBackupCategoryUpdateRules(): BackupCategoryUpdateRules? {
    if (isDefault) return null
    val skipRules = overrides?.skipRules ?: LibraryUpdateSkipRules.None
    return BackupCategoryUpdateRules(
        autoUpdate = autoUpdate,
        customRules = overrides != null,
        skipCompleted = skipRules.skipCompleted,
        skipUnseen = skipRules.skipUnseen,
        skipNotStarted = skipRules.skipNotStarted,
        skipOutsideReleasePeriod = skipRules.skipOutsideReleasePeriod,
        intervalHours = overrides?.intervalHours,
    )
}

val backupCategoryMapper = { category: Category ->
    BackupCategory(
        id = category.id,
        name = category.name,
        order = category.order,
        flags = category.flags,
    )
}
