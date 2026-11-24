package app.katari.sync.client.server

import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory

/**
 * [delegate]'s sockets with Nagle's algorithm off. OkHttp writes a request's headers and body in separate writes, so
 * with Nagle on, the body's last segment waits for the server to acknowledge the first, which the server delays by up
 * to 40 ms while it waits for the rest of the request: every push larger than a segment took that long whatever its
 * size, which made it most of a first sync's upload.
 */
internal class NoDelaySocketFactory(private val delegate: SocketFactory) : SocketFactory() {
    override fun createSocket(): Socket = delegate.createSocket().noDelay()

    override fun createSocket(host: String, port: Int): Socket = delegate.createSocket(host, port).noDelay()

    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket =
        delegate.createSocket(host, port, localHost, localPort).noDelay()

    override fun createSocket(host: InetAddress, port: Int): Socket = delegate.createSocket(host, port).noDelay()

    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket =
        delegate.createSocket(address, port, localAddress, localPort).noDelay()

    private fun Socket.noDelay(): Socket = apply { tcpNoDelay = true }
}
