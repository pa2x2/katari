package mihon.desktop.source

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import coil3.compose.SubcomposeAsyncImage
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.browse.components.SourceRows
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import mihon.desktop.image.InstalledExtensionIcon
import mihon.desktop.image.InstalledSourceIcon
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.Dangerous
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.LogoFrame
import mihon.presentation.kit.content.LogoSize
import mihon.presentation.kit.surface.LetterTile
import mihon.presentation.kit.theme.ContentTone
import tachiyomi.domain.source.model.Source

/**
 * The icons and stores of the extensions desktop loads, as Android draws them: a listing's icon from its store, an
 * installed extension's own, and a warning for one that isn't loaded. A source whose extension isn't installed, or an
 * icon that can't be drawn, shows its name's initial.
 */
@Inject
class DesktopSourceRows(
    private val extensionManager: ExtensionManager,
) : SourceRows {
    @Composable
    override fun Icon(source: Source, size: LogoSize, modifier: Modifier) {
        val name = source.name.ifBlank { source.id.toString() }
        LogoFrame(size = size, modifier = modifier) {
            if (source.isStub) {
                LetterTile(name = name, modifier = Modifier.fillMaxSize())
            } else {
                IconOrInitial(model = InstalledSourceIcon(source.id), name = name)
            }
        }
    }

    @Composable
    override fun Icon(sourceId: Long, name: String, modifier: Modifier) {
        LogoFrame(size = LogoSize.Row, modifier = modifier) {
            IconOrInitial(model = InstalledSourceIcon(sourceId), name = name.ifBlank { sourceId.toString() })
        }
    }

    @Composable
    override fun ExtensionIcon(extension: Extension, size: LogoSize, modifier: Modifier) {
        LogoFrame(size = size, modifier = modifier) {
            when (extension) {
                is Extension.Available -> IconOrInitial(model = extension.iconUrl, name = extension.name)
                is Extension.Loaded -> IconOrInitial(
                    model = InstalledExtensionIcon(extension.pkgName, extension.form, extension.versionCode),
                    name = extension.name,
                )
                is Extension.NotLoaded -> Icon(
                    imageVector = MaterialSymbols.RoundedFilled.Dangerous,
                    contentDescription = null,
                    tone = ContentTone.Error,
                )
            }
        }
    }

    @Composable
    override fun storeName(sourceId: Long): String? {
        val storeNames by extensionManager.sourceStoreNames.collectAsState()
        return storeNames[sourceId]
    }

    @Composable
    private fun IconOrInitial(model: Any, name: String) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = null,
            loading = { LetterTile(name = name, modifier = Modifier.fillMaxSize()) },
            error = { LetterTile(name = name, modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
