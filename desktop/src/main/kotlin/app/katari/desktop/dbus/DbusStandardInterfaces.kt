package app.katari.desktop.dbus

import java.nio.file.Files
import java.nio.file.Path

/**
 * What every object the app serves on a bus answers besides its own interfaces: `Peer`, which the bus's tools ping,
 * `Properties`, through which services read its properties, which are read-only, and `Introspectable`, whose document
 * starts with [INTROSPECTION_HEAD], and the errors those answers use.
 */
internal object DbusStandardInterfaces {
    const val PROPERTIES = "org.freedesktop.DBus.Properties"
    const val INTROSPECTABLE = "org.freedesktop.DBus.Introspectable"
    const val PEER = "org.freedesktop.DBus.Peer"

    const val UNKNOWN_OBJECT = "org.freedesktop.DBus.Error.UnknownObject"
    const val UNKNOWN_INTERFACE = "org.freedesktop.DBus.Error.UnknownInterface"
    const val UNKNOWN_METHOD = "org.freedesktop.DBus.Error.UnknownMethod"
    const val UNKNOWN_PROPERTY = "org.freedesktop.DBus.Error.UnknownProperty"
    const val PROPERTY_READ_ONLY = "org.freedesktop.DBus.Error.PropertyReadOnly"
    const val NOT_SUPPORTED = "org.freedesktop.DBus.Error.NotSupported"
    const val FAILED = "org.freedesktop.DBus.Error.Failed"
    const val SERVICE_UNKNOWN = "org.freedesktop.DBus.Error.ServiceUnknown"
    const val INVALID_ARGS = "org.freedesktop.DBus.Error.InvalidArgs"

    /** The members of the standard interfaces, by name, for calls that leave out their interface. */
    val INTERFACE_OF_MEMBER: Map<String, String> = buildMap {
        listOf("Get", "GetAll", "Set").forEach { put(it, PROPERTIES) }
        put("Introspect", INTROSPECTABLE)
        listOf("Ping", "GetMachineId").forEach { put(it, PEER) }
    }

    /** An object's introspection document up to its own interfaces, which follow before `</node>`. */
    const val INTROSPECTION_HEAD = """<!DOCTYPE node PUBLIC "-//freedesktop//DTD D-BUS Object Introspection 1.0//EN"
 "http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd">
<node>
  <interface name="org.freedesktop.DBus.Introspectable">
    <method name="Introspect"><arg name="xml_data" type="s" direction="out"/></method>
  </interface>
  <interface name="org.freedesktop.DBus.Peer">
    <method name="Ping"/>
    <method name="GetMachineId"><arg name="machine_uuid" type="s" direction="out"/></method>
  </interface>
  <interface name="org.freedesktop.DBus.Properties">
    <method name="Get">
      <arg name="interface_name" type="s" direction="in"/>
      <arg name="property_name" type="s" direction="in"/>
      <arg name="value" type="v" direction="out"/>
    </method>
    <method name="GetAll">
      <arg name="interface_name" type="s" direction="in"/>
      <arg name="properties" type="a{sv}" direction="out"/>
    </method>
    <method name="Set">
      <arg name="interface_name" type="s" direction="in"/>
      <arg name="property_name" type="s" direction="in"/>
      <arg name="value" type="v" direction="in"/>
    </method>
    <signal name="PropertiesChanged">
      <arg name="interface_name" type="s"/>
      <arg name="changed_properties" type="a{sv}"/>
      <arg name="invalidated_properties" type="as"/>
    </signal>
  </interface>
"""

    fun answerPeer(bus: DbusConnection, call: DbusMessage) {
        when (call.member) {
            "Ping" -> bus.reply(call)
            "GetMachineId" -> {
                val id = MACHINE_ID_FILES.firstNotNullOfOrNull { file ->
                    runCatching { Files.readString(file).trim() }.getOrNull()?.takeIf(String::isNotEmpty)
                }
                if (id != null) bus.reply(call, "s", id) else bus.replyError(call, FAILED, "No machine ID")
            }
            else -> bus.replyError(call, UNKNOWN_METHOD, "No method ${call.member}")
        }
    }

    /** Answers a `Properties` [call] from [propertiesOf], an interface's properties, or null for one it doesn't have. */
    fun answerProperties(bus: DbusConnection, call: DbusMessage, propertiesOf: (String?) -> Map<String, Variant>?) {
        val iface = call.body.firstOrNull() as? String
        val properties = propertiesOf(iface) ?: return bus.replyError(call, UNKNOWN_INTERFACE, "No interface $iface")
        when (call.member) {
            "Get" -> {
                val name = call.body.getOrNull(1) as? String
                val value = properties[name] ?: return bus.replyError(call, UNKNOWN_PROPERTY, "No property $name")
                bus.reply(call, "v", value)
            }
            "GetAll" -> bus.reply(call, "a{sv}", properties)
            "Set" -> bus.replyError(call, PROPERTY_READ_ONLY, "The properties are read-only")
            else -> bus.replyError(call, UNKNOWN_METHOD, "No method ${call.member}")
        }
    }

    private val MACHINE_ID_FILES = listOf(Path.of("/etc/machine-id"), Path.of("/var/lib/dbus/machine-id"))
}
