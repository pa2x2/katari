package app.katari.sync.protocol.device

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * A linked device, picked at random over all 64 bits by the server when the device registers, and never reused. It
 * breaks ties between timestamps from different devices, so it must differ from every id another server gave out
 * before: a device registering with a replacement server gets a new one while old field timestamps keep the old.
 * Encode it as fixed64, which random values fit better than a varint.
 */
@Serializable
@JvmInline
value class DeviceId(val value: Long)
