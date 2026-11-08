package mihon.entry.interactions.reader.image.export

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
@BindingContainer
object ReaderImageExportBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun imageExport(application: Application): ReaderImageExport = AndroidReaderImageExport(application)
}
