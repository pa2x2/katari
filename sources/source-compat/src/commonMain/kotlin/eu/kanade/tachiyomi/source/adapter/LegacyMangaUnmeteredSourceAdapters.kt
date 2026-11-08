package eu.kanade.tachiyomi.source.adapter

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.entry.UnmeteredSource
import eu.kanade.tachiyomi.source.online.HttpSource

internal class LegacyMangaUnmeteredSourceAdapter(
    source: Source,
) : LegacyMangaSourceAdapter(source), UnmeteredSource

internal class LegacyMangaUnmeteredCatalogueSourceAdapter(
    source: CatalogueSource,
) : LegacyMangaCatalogueSourceAdapter(source), UnmeteredSource

internal class LegacyMangaUnmeteredRelatedCatalogueSourceAdapter(
    source: CatalogueSource,
) : LegacyMangaRelatedCatalogueSourceAdapter(source), UnmeteredSource

internal class LegacyMangaUnmeteredWebViewCatalogueSourceAdapter(
    source: HttpSource,
) : LegacyMangaWebViewCatalogueSourceAdapter(source), UnmeteredSource

internal class LegacyMangaUnmeteredRelatedWebViewCatalogueSourceAdapter(
    source: HttpSource,
) : LegacyMangaRelatedWebViewCatalogueSourceAdapter(source), UnmeteredSource

internal class LegacyMangaUnmeteredConfigurableSourceAdapter(
    source: Source,
) : LegacyMangaConfigurableSourceAdapter(source), UnmeteredSource

internal class LegacyMangaUnmeteredConfigurableCatalogueSourceAdapter(
    source: CatalogueSource,
) : LegacyMangaConfigurableCatalogueSourceAdapter(source), UnmeteredSource

internal class LegacyMangaUnmeteredRelatedConfigurableCatalogueSourceAdapter(
    source: CatalogueSource,
) : LegacyMangaRelatedConfigurableCatalogueSourceAdapter(source), UnmeteredSource

internal class LegacyMangaUnmeteredWebViewConfigurableCatalogueSourceAdapter(
    source: HttpSource,
) : LegacyMangaWebViewConfigurableCatalogueSourceAdapter(source), UnmeteredSource

internal class LegacyMangaUnmeteredRelatedWebViewConfigurableCatalogueSourceAdapter(
    source: HttpSource,
) : LegacyMangaRelatedWebViewConfigurableCatalogueSourceAdapter(source), UnmeteredSource
