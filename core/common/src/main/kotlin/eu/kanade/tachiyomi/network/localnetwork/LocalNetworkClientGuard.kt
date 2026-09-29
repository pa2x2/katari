package eu.kanade.tachiyomi.network.localnetwork

import android.content.Context
import okhttp3.Dns
import okhttp3.OkHttpClient
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import javax.net.SocketFactory

/**
 * Fails connections to the local network fast while Android withholds the local network permission, and asks the UI
 * to request it. Without this, blocked TCP connections only surface as a connect timeout.
 *
 * Checks happen when a socket connects, which covers IP literals that bypass [Dns], and when a multicast DNS name is
 * resolved, which fails before any socket exists.
 */
fun OkHttpClient.guardingLocalNetworkAccess(context: Context): OkHttpClient {
    require(socketFactory === SocketFactory.getDefault()) { "The local network guard replaces the socket factory" }
    val guard = LocalNetworkGuard(context.applicationContext)
    return newBuilder()
        .socketFactory(guard.GuardedSocketFactory())
        .dns(guard.GuardedDns(dns))
        .build()
}

private class LocalNetworkGuard(private val context: Context) {

    fun checkHost(host: String) {
        if (LocalNetworkAccess.isGranted(context)) return
        LocalNetworkAccess.requestPermission()
        throw LocalNetworkAccessDeniedException(
            context.stringResource(MR.strings.local_network_access_denied, host),
        )
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
