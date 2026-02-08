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
        // Skip if packet is too small
        if (packet.size < 20) return
        
        // Get IP header info
        val version = (packet[0].toInt() and 0xF0) shr 4
        if (version != 4) return // Only IPv4 for now
        
        val protocol = packet[9].toInt() and 0xFF
        val ipHeaderLength = (packet[0].toInt() and 0x0F) * 4
        
        // Check destination port for DNS (UDP port 53)
        if (protocol == UDP_PROTOCOL && packet.size >= ipHeaderLength + 4) {
            val destPort = ((packet[ipHeaderLength + 2].toInt() and 0xFF) shl 8) or
                          (packet[ipHeaderLength + 3].toInt() and 0xFF)
            
            if (destPort == DNS_PORT) {
                // DNS query - process through our filter
                val dnsQuery = dnsParser.parsePacket(packet)
                if (dnsQuery != null) {
                    processDnsQuery(packet, dnsQuery)
                    return
                }
            }
        }
        
        // Non-DNS traffic - allow to pass through unchanged
        // For VPN to work properly with non-DNS traffic, we need the traffic
        // to be routed outside the VPN tunnel
        // This is handled by not routing non-VPN traffic through our interface
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
        try {
            val ipHeaderLength = 20 // Standard IPv4 header without options
            
            // Swap source and destination addresses
            val srcAddr = originalPacket.copyOfRange(12, 16)
            val dstAddr = originalPacket.copyOfRange(16, 20)
            
            // Get original ports
            val origIpHeaderLen = (originalPacket[0].toInt() and 0x0F) * 4
            val srcPort = ByteBuffer.wrap(originalPacket, origIpHeaderLen, 2).short
            val dstPort = ByteBuffer.wrap(originalPacket, origIpHeaderLen + 2, 2).short

            val udpLength = 8 + dnsResponse.size
            val totalLength = ipHeaderLength + udpLength

            val responsePacket = ByteBuffer.allocate(totalLength).order(ByteOrder.BIG_ENDIAN)

            // IP Header (swap src/dst)
            responsePacket.put((0x45).toByte()) // Version (4) + IHL (5 = 20 bytes)
            responsePacket.put(0)                // DSCP/ECN
            responsePacket.putShort(totalLength.toShort())
            responsePacket.putShort(0)           // Identification
            responsePacket.putShort(0x4000.toShort()) // Flags + Fragment offset (Don't fragment)
            responsePacket.put(64)               // TTL
            responsePacket.put(UDP_PROTOCOL.toByte())
            responsePacket.putShort(0)           // Checksum placeholder (calculated below)
            responsePacket.put(dstAddr)          // Swapped: original dst is now src
            responsePacket.put(srcAddr)          // Swapped: original src is now dst

            // UDP Header (swap ports)
            responsePacket.putShort(dstPort)     // Source port (was destination)
            responsePacket.putShort(srcPort)     // Destination port (was source)
            responsePacket.putShort(udpLength.toShort())
            responsePacket.putShort(0)           // UDP Checksum (optional for IPv4)

            // DNS Response
            responsePacket.put(dnsResponse)

            // Calculate and set IP header checksum
            val packetArray = responsePacket.array()
            val checksum = calculateIpChecksum(packetArray, ipHeaderLength)
            packetArray[10] = ((checksum shr 8) and 0xFF).toByte()
            packetArray[11] = (checksum and 0xFF).toByte()

            Log.d(TAG, "Response packet built: ${packetArray.size} bytes, checksum: ${String.format("%04X", checksum)}")

            return packetArray
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build response packet", e)
            return null
        }
    }

    /**
     * Calculate IP header checksum (RFC 1071).
     */
    private fun calculateIpChecksum(header: ByteArray, length: Int): Int {
        var sum = 0
        var i = 0
        
        while (i < length) {
            // Skip checksum field at bytes 10-11
            if (i == 10) {
                i += 2
                continue
            }
            
            val high = (header[i].toInt() and 0xFF) shl 8
            val low = if (i + 1 < length) header[i + 1].toInt() and 0xFF else 0
            sum += high + low
            i += 2
        }
        
        // Fold 32-bit sum to 16 bits
        while ((sum shr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        
        return sum.inv() and 0xFFFF
    }
}
