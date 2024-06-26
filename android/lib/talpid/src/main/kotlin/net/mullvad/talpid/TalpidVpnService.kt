package net.mullvad.talpid

import android.os.ParcelFileDescriptor
import androidx.annotation.CallSuper
import co.touchlab.kermit.Logger
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import kotlin.properties.Delegates.observable
import net.mullvad.talpid.model.CreateTunResult
import net.mullvad.talpid.model.TunConfig
import net.mullvad.talpid.util.TalpidSdkUtils.setMeteredIfSupported

open class TalpidVpnService : LifecycleVpnService() {
    private var activeTunStatus by
        observable<CreateTunResult?>(null) { _, oldTunStatus, _ ->
            val oldTunFd =
                when (oldTunStatus) {
                    is CreateTunResult.Success -> oldTunStatus.tunFd
                    is CreateTunResult.InvalidDnsServers -> oldTunStatus.tunFd
                    else -> null
                }

            if (oldTunFd != null) {
                ParcelFileDescriptor.adoptFd(oldTunFd).close()
            }
        }

    private val tunIsOpen
        get() = activeTunStatus?.isOpen ?: false

    private var currentTunConfig = defaultTunConfig()

    // Used by JNI
    val connectivityListener = ConnectivityListener()

    @CallSuper
    override fun onCreate() {
        super.onCreate()
        connectivityListener.register(this)
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        connectivityListener.unregister()
    }

    fun getTun(config: TunConfig): CreateTunResult {
        synchronized(this) {
            val tunStatus = activeTunStatus

            if (config == currentTunConfig && tunIsOpen) {
                return tunStatus!!
            } else {
                val newTunStatus = createTun(config)

                currentTunConfig = config
                activeTunStatus = newTunStatus

                return newTunStatus
            }
        }
    }

    fun createTun() {
        synchronized(this) { activeTunStatus = createTun(currentTunConfig) }
    }

    fun recreateTunIfOpen(config: TunConfig) {
        synchronized(this) {
            if (tunIsOpen) {
                currentTunConfig = config
                activeTunStatus = createTun(config)
            }
        }
    }

    fun closeTun() {
        synchronized(this) { activeTunStatus = null }
    }

    private fun createTun(config: TunConfig): CreateTunResult {
        if (prepare(this) != null) {
            // VPN permission wasn't granted
            return CreateTunResult.PermissionDenied
        }

        val invalidDnsServerAddresses = ArrayList<InetAddress>()

        val builder =
            Builder().apply {
                for (address in config.addresses) {
                    addAddress(address, prefixForAddress(address))
                }

                for (dnsServer in config.dnsServers) {
                    try {
                        addDnsServer(dnsServer)
                    } catch (exception: IllegalArgumentException) {
                        invalidDnsServerAddresses.add(dnsServer)
                    }
                }

                // Avoids creating a tunnel with no DNS servers or if all DNS servers was invalid,
                // since apps then may leak DNS requests.
                // https://issuetracker.google.com/issues/337961996
                if (invalidDnsServerAddresses.size == config.dnsServers.size) {
                    Logger.w(
                        "All DNS servers invalid or non set, using fallback DNS server to " +
                            "minimize leaks, dnsServers.isEmpty(): ${config.dnsServers.isEmpty()}"
                    )
                    addDnsServer(FALLBACK_DUMMY_DNS_SERVER)
                }

                for (route in config.routes) {
                    addRoute(route.address, route.prefixLength.toInt())
                }

                config.excludedPackages.forEach { app -> addDisallowedApplication(app) }
                setMtu(config.mtu)
                setBlocking(false)
                setMeteredIfSupported(false)
            }

        val vpnInterface = builder.establish()
        val tunFd = vpnInterface?.detachFd()

        if (tunFd == null) {
            return CreateTunResult.TunnelDeviceError
        }

        waitForTunnelUp(tunFd, config.routes.any { route -> route.isIpv6 })

        if (invalidDnsServerAddresses.isNotEmpty()) {
            return CreateTunResult.InvalidDnsServers(invalidDnsServerAddresses, tunFd)
        }

        return CreateTunResult.Success(tunFd)
    }

    fun bypass(socket: Int): Boolean {
        return protect(socket)
    }

    private fun prefixForAddress(address: InetAddress): Int {
        return when (address) {
            is Inet4Address -> 32
            is Inet6Address -> 128
            else -> throw IllegalArgumentException("Invalid IP address (not IPv4 nor IPv6)")
        }
    }

    private external fun defaultTunConfig(): TunConfig

    private external fun waitForTunnelUp(tunFd: Int, isIpv6Enabled: Boolean)

    companion object {
        private const val FALLBACK_DUMMY_DNS_SERVER = "192.0.2.1"
    }
}
