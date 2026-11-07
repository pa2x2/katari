package app.katari.sync.server.web.page

import app.katari.sync.protocol.device.DeviceDescription
import kotlinx.html.FlowContent
import kotlinx.html.ThScope
import kotlinx.html.p
import kotlinx.html.span
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

internal class DeviceRow(val id: Long, val description: DeviceDescription, val lastSeenMillis: Long)

/** An account's linked devices, each with [action] in its last column when there is one. */
internal fun FlowContent.deviceTable(
    devices: List<DeviceRow>,
    nowMillis: Long,
    emptyText: String,
    action: (FlowContent.(DeviceRow) -> Unit)? = null,
) {
    if (devices.isEmpty()) {
        p("sub empty") { +emptyText }
        return
    }
    table("list devices") {
        thead {
            tr {
                th(ThScope.col) { +"Device" }
                th(ThScope.col) { +"App" }
                th(ThScope.col) { +"Last sync" }
                if (action != null) th(ThScope.col) { span("hidden-label") { +"Remove" } }
            }
        }
        tbody {
            devices.forEach { device ->
                tr {
                    td("name") { +device.description.name }
                    td {
                        attributes["data-label"] = "App"
                        +"${device.description.platform} · ${device.description.appVersion}"
                    }
                    td {
                        attributes["data-label"] = "Last sync"
                        +lastSyncText(device.lastSeenMillis, nowMillis)
                    }
                    if (action != null) td("row-action") { action(device) }
                }
            }
        }
    }
}
