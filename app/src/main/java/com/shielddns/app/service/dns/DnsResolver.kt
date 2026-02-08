package com.shielddns.app.service.dns

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves DNS queries by forwarding to upstream DNS servers.
 */
@Singleton
class DnsResolver @Inject constructor() {

    companion object {
        private const val DNS_PORT = 53
        private const val TIMEOUT_MS = 5000
        private const val MAX_RESPONSE_SIZE = 512
        
        val DEFAULT_DNS_SERVERS = listOf(
            "8.8.8.8",      // Google Primary
            "8.8.4.4",      // Google Secondary
            "1.1.1.1",      // Cloudflare Primary
            "1.0.0.1"       // Cloudflare Secondary
        )
    }

    private var currentDnsServer: String = DEFAULT_DNS_SERVERS[0]

    /**
     * Set the upstream DNS server to use.
     */
    fun setDnsServer(server: String) {
        currentDnsServer = server
    }

    /**
     * Forward a DNS query to the upstream server and return the response.
     * 
     * @param dnsPayload The DNS query payload (without IP/UDP headers)
     * @return DNS response payload, or null if resolution failed
     */
    suspend fun resolve(dnsPayload: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket().apply {
                soTimeout = TIMEOUT_MS
            }

            val serverAddress = InetAddress.getByName(currentDnsServer)
            val requestPacket = DatagramPacket(
                dnsPayload,
                dnsPayload.size,
                serverAddress,
                DNS_PORT
            )

            socket.send(requestPacket)

            val responseBuffer = ByteArray(MAX_RESPONSE_SIZE)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(responsePacket)

            // Return only the actual response data
            responseBuffer.copyOfRange(0, responsePacket.length)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            socket?.close()
        }
    }

    /**
     * Extract DNS payload from IP packet.
     */
    fun extractDnsPayload(packet: ByteArray): ByteArray? {
        if (packet.size < 28) return null // Min IP + UDP headers

        val ipHeaderLength = (packet[0].toInt() and 0x0F) * 4
        val udpOffset = ipHeaderLength
        
        // Get UDP length
        if (packet.size < udpOffset + 8) return null
        val udpLength = ByteBuffer.wrap(packet, udpOffset + 4, 2).short.toInt() and 0xFFFF
        val dnsLength = udpLength - 8 // Subtract UDP header

        if (dnsLength <= 0) return null

        val dnsOffset = ipHeaderLength + 8
        if (packet.size < dnsOffset + dnsLength) return null

        return packet.copyOfRange(dnsOffset, dnsOffset + dnsLength)
    }
}
