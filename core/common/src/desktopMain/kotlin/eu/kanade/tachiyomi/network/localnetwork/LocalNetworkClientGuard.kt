package eu.kanade.tachiyomi.network.localnetwork

import android.content.Context
import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import javax.net.SocketFactory

/**
 * Android's guard, which fails connections to the local network fast while Android withholds the local network
 * permission, kept as Android has it for extensions that put it on their own clients. Desktop has no such permission
 * ([LocalNetworkAccess.isGranted]), so the guarded client connects wherever the original would.
 */
fun OkHttpClient.guardingLocalNetworkAccess(context: Context, deniedMessage: (host: String) -> String): OkHttpClient {
    require(socketFactory === SocketFactory.getDefault()) { "The local network guard replaces the socket factory" }
    val guard = LocalNetworkGuard(context.applicationContext, deniedMessage)
    return newBuilder()
        .socketFactory(guard.GuardedSocketFactory())
        .dns(guard.GuardedDns(dns))
        .build()
}

private class LocalNetworkGuard(
    private val context: Context,
    private val deniedMessage: (host: String) -> String,
) {

    fun checkHost(host: String) {
        if (LocalNetworkAccess.isGranted(context)) return
        LocalNetworkAccess.requestPermission()
        throw LocalNetworkAccessDeniedException(deniedMessage(host))
    }

    inner class GuardedDns(private val delegate: Dns) : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            if (isMulticastDnsHostName(hostname)) checkHost(hostname)
            return delegate.lookup(hostname)
        }
    }

    inner class GuardedSocketFactory : SocketFactory() {
        override fun createSocket(): Socket = GuardedSocket()

        override fun createSocket(host: String, port: Int): Socket {
            return GuardedSocket().apply { connect(InetSocketAddress(host, port)) }
        }

        override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
            return GuardedSocket().apply {
                bind(InetSocketAddress(localHost, localPort))
                connect(InetSocketAddress(host, port))
            }
        }

        override fun createSocket(host: InetAddress, port: Int): Socket {
            return GuardedSocket().apply { connect(InetSocketAddress(host, port)) }
        }

        override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
            return GuardedSocket().apply {
                bind(InetSocketAddress(localAddress, localPort))
                connect(InetSocketAddress(address, port))
            }
        }
    }

    private inner class GuardedSocket : Socket() {
        override fun connect(endpoint: SocketAddress, timeout: Int) {
            if (endpoint is InetSocketAddress && endpoint.address?.isLocalNetworkAddress() == true) {
                checkHost(endpoint.hostString)
            }
            super.connect(endpoint, timeout)
        }
    }
}
