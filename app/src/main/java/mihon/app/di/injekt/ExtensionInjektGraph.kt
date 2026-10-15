package mihon.app.di.injekt

import eu.kanade.tachiyomi.network.JavaScriptEngine
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.EntryPreferenceProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import nl.adaptivity.xmlutil.serialization.XML

/** The graph services extensions resolve through Injekt, which [MetroInjektRegistrar] serves. */
interface ExtensionInjektGraph {
    val json: Json
    val protoBuf: ProtoBuf
    val xml: XML
    val networkHelper: NetworkHelper
    val javaScriptEngine: JavaScriptEngine
    val entryPreferenceProvider: EntryPreferenceProvider
}
