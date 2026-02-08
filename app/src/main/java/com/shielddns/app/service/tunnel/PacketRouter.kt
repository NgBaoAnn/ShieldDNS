package com.shielddns.app.service.tunnel

import android.util.Log
import com.shielddns.app.domain.model.DnsQuery
import com.shielddns.app.service.dns.DnsPacketParser
import com.shielddns.app.service.dns.DnsQueryBuilder
import com.shielddns.app.service.dns.DnsResolver
import com.shielddns.app.service.filter.BlocklistFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Routes packets through the VPN tunnel, filtering DNS queries.
 */
class PacketRouter(
    private val tunInterface: TunInterface,
    private val dnsParser: DnsPacketParser,
    private val dnsResolver: DnsResolver,
    private val dnsBuilder: DnsQueryBuilder,
    private val blocklistFilter: BlocklistFilter,
    private val onQueryBlocked: (DnsQuery) -> Unit = {},
    private val onQueryAllowed: (DnsQuery) -> Unit = {}
) {
    companion object {
        private const val TAG = "PacketRouter"
        private const val UDP_PROTOCOL = 17
        private const val DNS_PORT = 53
    }

    private var routerJob: Job? = null

    /**
     * Start the packet routing loop.
     */
    fun start(scope: CoroutineScope) {
        routerJob = scope.launch(Dispatchers.IO) {
            Log.d(TAG, "Packet router started")
            
            while (isActive) {
                try {
                    val packet = tunInterface.readPacket()
                    if (packet != null && packet.isNotEmpty()) {
                        processPacket(packet)
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(TAG, "Error processing packet", e)
                    }
                }
            }
            
            Log.d(TAG, "Packet router stopped")
        }
    }

    /**
     * Stop the packet routing loop.
     */
    suspend fun stop() {
        routerJob?.cancelAndJoin()
        routerJob = null
    }

    private suspend fun processPacket(packet: ByteArray) {
        // Check if it's a DNS query
        val dnsQuery = dnsParser.parsePacket(packet)
        
        if (dnsQuery != null) {
            processDnsQuery(packet, dnsQuery)
        } else {
            // Non-DNS packet - forward unchanged
            // In a full implementation, we'd need to handle non-DNS traffic
            // For now, we only handle DNS
        }
    }

    private suspend fun processDnsQuery(originalPacket: ByteArray, query: DnsQuery) {
        Log.d(TAG, "DNS query: ${query.domain}")

        if (blocklistFilter.shouldBlock(query.domain)) {
            // Block this domain
            Log.d(TAG, "BLOCKED: ${query.domain}")
            onQueryBlocked(query)
            
            // Send blocked response (0.0.0.0)
            val ipHeaderLength = (originalPacket[0].toInt() and 0x0F) * 4
            val blockedResponse = dnsBuilder.buildBlockedResponse(query, originalPacket, ipHeaderLength)
            
            if (blockedResponse != null) {
                val responsePacket = buildResponsePacket(originalPacket, blockedResponse)
                if (responsePacket != null) {
                    tunInterface.writePacket(responsePacket)
                }
            }
        } else {
            // Allow this domain - forward to upstream DNS
            Log.d(TAG, "ALLOWED: ${query.domain}")
            onQueryAllowed(query)
            
            val dnsPayload = dnsResolver.extractDnsPayload(originalPacket)
            if (dnsPayload != null) {
                val response = dnsResolver.resolve(dnsPayload)
                if (response != null) {
                    val responsePacket = buildResponsePacket(originalPacket, response)
                    if (responsePacket != null) {
                        tunInterface.writePacket(responsePacket)
                    }
                }
            }
        }
    }

    /**
     * Build a complete IP/UDP response packet from DNS payload.
     */
    private fun buildResponsePacket(originalPacket: ByteArray, dnsResponse: ByteArray): ByteArray? {
        val ipHeaderLength = (originalPacket[0].toInt() and 0x0F) * 4
        
        // Swap source and destination addresses
        val srcAddr = originalPacket.copyOfRange(12, 16)
        val dstAddr = originalPacket.copyOfRange(16, 20)
        
        // Get original ports
        val srcPort = ByteBuffer.wrap(originalPacket, ipHeaderLength, 2).short
        val dstPort = ByteBuffer.wrap(originalPacket, ipHeaderLength + 2, 2).short

        val udpLength = 8 + dnsResponse.size
        val totalLength = ipHeaderLength + udpLength

        val responsePacket = ByteBuffer.allocate(totalLength).order(ByteOrder.BIG_ENDIAN)

        // IP Header (swap src/dst)
        responsePacket.put((0x45).toByte()) // Version + IHL
        responsePacket.put(0)                // DSCP/ECN
        responsePacket.putShort(totalLength.toShort())
        responsePacket.putShort(0)           // Identification
        responsePacket.putShort(0x4000.toShort()) // Flags + Fragment offset (Don't fragment)
        responsePacket.put(64)               // TTL
        responsePacket.put(UDP_PROTOCOL.toByte())
        responsePacket.putShort(0)           // Checksum (will be calculated by kernel)
        responsePacket.put(dstAddr)          // Swapped: original dst is now src
        responsePacket.put(srcAddr)          // Swapped: original src is now dst

        // UDP Header (swap ports)
        responsePacket.putShort(dstPort)     // Source port (was destination)
        responsePacket.putShort(srcPort)     // Destination port (was source)
        responsePacket.putShort(udpLength.toShort())
        responsePacket.putShort(0)           // Checksum (optional for UDP over IPv4)

        // DNS Response
        responsePacket.put(dnsResponse)

        return responsePacket.array()
    }
}
