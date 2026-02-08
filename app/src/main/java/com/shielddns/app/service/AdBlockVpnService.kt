package com.shielddns.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.shielddns.app.R
import com.shielddns.app.domain.model.DnsQuery
import com.shielddns.app.presentation.MainActivity
import com.shielddns.app.presentation.state.VpnConnectionState
import com.shielddns.app.service.dns.DnsPacketParser
import com.shielddns.app.service.dns.DnsQueryBuilder
import com.shielddns.app.service.dns.DnsResolver
import com.shielddns.app.service.filter.BlocklistFilter
import com.shielddns.app.service.tunnel.PacketRouter
import com.shielddns.app.service.tunnel.TunInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * VPN Service for DNS-based ad blocking.
 * 
 * Creates a TUN interface to intercept DNS queries and filter
 * ad/tracking domains by returning null routes (0.0.0.0).
 */
@AndroidEntryPoint
class AdBlockVpnService : VpnService() {

    companion object {
        private const val TAG = "AdBlockVpnService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "shield_dns_vpn"
        
        const val ACTION_START = "com.shielddns.app.START_VPN"
        const val ACTION_STOP = "com.shielddns.app.STOP_VPN"
        
        // VPN Configuration
        private const val VPN_ADDRESS = "10.0.0.1"
        private const val VPN_ROUTE = "0.0.0.0"
        private const val VPN_DNS = "8.8.8.8"
        private const val VPN_MTU = 1500
        
        private val _connectionState = MutableStateFlow<VpnConnectionState>(VpnConnectionState.Disconnected)
        val connectionState: StateFlow<VpnConnectionState> = _connectionState.asStateFlow()
        
        private val _blockedCount = MutableStateFlow(0L)
        val blockedCount: StateFlow<Long> = _blockedCount.asStateFlow()
    }

    @Inject lateinit var dnsResolver: DnsResolver
    @Inject lateinit var blocklistFilter: BlocklistFilter

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunInterface: TunInterface? = null
    private var packetRouter: PacketRouter? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")
        
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
            else -> {
                // Default to start if no action specified
                startVpn()
            }
        }
        
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        cleanupVpn()
        serviceScope.cancel()
    }

    override fun onRevoke() {
        super.onRevoke()
        Log.d(TAG, "VPN revoked by system")
        stopVpn()
    }

    private fun startVpn() {
        _connectionState.value = VpnConnectionState.Connecting
        startForeground(NOTIFICATION_ID, createNotification())
        
        serviceScope.launch {
            try {
                establishVpn()
                _connectionState.value = VpnConnectionState.Connected
                Log.d(TAG, "VPN connected successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to establish VPN", e)
                _connectionState.value = VpnConnectionState.Error(e.message ?: "Unknown error")
                stopSelf()
            }
        }
    }

    private fun stopVpn() {
        Log.d(TAG, "Stopping VPN")
        cleanupVpn()
        _connectionState.value = VpnConnectionState.Disconnected
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun establishVpn() {
        val builder = Builder()
            .setSession("ShieldDNS")
            .addAddress(VPN_ADDRESS, 24)
            .addDnsServer(VPN_DNS)
            .addRoute(VPN_ROUTE, 0)
            .setMtu(VPN_MTU)
            .setBlocking(true)
        
        // Allow this app to bypass VPN (prevent loops)
        builder.addDisallowedApplication(packageName)

        vpnInterface = builder.establish()
            ?: throw IllegalStateException("Failed to establish VPN interface")

        Log.d(TAG, "VPN interface established")

        // Setup TUN interface and packet router
        tunInterface = TunInterface(vpnInterface!!)
        
        val dnsParser = DnsPacketParser()
        val dnsBuilder = DnsQueryBuilder()

        packetRouter = PacketRouter(
            tunInterface = tunInterface!!,
            dnsParser = dnsParser,
            dnsResolver = dnsResolver,
            dnsBuilder = dnsBuilder,
            blocklistFilter = blocklistFilter,
            onQueryBlocked = { query -> onDomainBlocked(query) },
            onQueryAllowed = { query -> onDomainAllowed(query) }
        )

        packetRouter?.start(serviceScope)
    }

    private fun cleanupVpn() {
        serviceScope.launch {
            packetRouter?.stop()
        }
        tunInterface?.close()
        tunInterface = null
        vpnInterface = null
        packetRouter = null
    }

    private fun onDomainBlocked(query: DnsQuery) {
        _blockedCount.value++
        Log.d(TAG, "Blocked: ${query.domain} (total: ${_blockedCount.value})")
    }

    private fun onDomainAllowed(query: DnsQuery) {
        Log.v(TAG, "Allowed: ${query.domain}")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ShieldDNS VPN",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows VPN connection status"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AdBlockVpnService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ShieldDNS Active")
            .setContentText("Protecting your device from ads and trackers")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }
}
