package eu.kanade.tachiyomi.ui.library

import tachiyomi.domain.library.model.LibraryItemKey

internal fun List<LibraryPage>.withTabItemCounts(): List<LibraryPage> {
    val primaryMemberships = mutableMapOf<String, MutableSet<LibraryItemKey>>()
    val secondaryMemberships = mutableMapOf<SecondaryTabPath, MutableSet<LibraryItemKey>>()
    forEach { page ->
        primaryMemberships.getOrPut(page.primaryTab.id, ::mutableSetOf) += page.itemIds
        page.secondaryTab?.let { secondaryTab ->
            secondaryMemberships.getOrPut(
                SecondaryTabPath(page.primaryTab.id, secondaryTab.id),
                ::mutableSetOf,
            ) += page.itemIds
        }
    }

    return map { page ->
        page.copy(
            primaryTab = page.primaryTab.copy(
                itemCount = primaryMemberships.getValue(page.primaryTab.id).size,
            ),
            secondaryTab = page.secondaryTab?.let { secondaryTab ->
                secondaryTab.copy(
                    itemCount = secondaryMemberships.getValue(
                        SecondaryTabPath(page.primaryTab.id, secondaryTab.id),
                    ).size,
                )
            },
            tertiaryTab = page.tertiaryTab?.copy(itemCount = page.itemIds.size),
        )
    }
}

private data class SecondaryTabPath(
    val primaryTabId: String,
    val secondaryTabId: String,
)
