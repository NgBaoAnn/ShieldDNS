package com.shielddns.app.service.dns

import com.shielddns.app.domain.model.DnsQuery
import com.shielddns.app.domain.model.DnsQuery.QueryType
import java.nio.ByteBuffer

/**
 * Parses DNS packets from raw byte arrays.
 * 
 * DNS packet structure:
 * - IP Header: 20 bytes (minimum)
 * - UDP Header: 8 bytes
 * - DNS Header: 12 bytes
 * - DNS Question section: variable length
 */
class DnsPacketParser {

    companion object {
        private const val IP_HEADER_MIN_SIZE = 20
        private const val UDP_HEADER_SIZE = 8
        private const val DNS_HEADER_SIZE = 12
        private const val UDP_PROTOCOL = 17
        private const val DNS_PORT = 53
    }

    /**
     * Parse a raw IP packet and extract DNS query if present.
     * 
     * @param packet Raw IP packet bytes
     * @return DnsQuery if packet is a valid DNS query, null otherwise
     */
    fun parsePacket(packet: ByteArray): DnsQuery? {
        if (packet.size < IP_HEADER_MIN_SIZE + UDP_HEADER_SIZE + DNS_HEADER_SIZE) {
            return null
        }

        // Check IP version (should be 4 for IPv4)
        val version = (packet[0].toInt() and 0xF0) shr 4
        if (version != 4) return null

        // Get IP header length (in 32-bit words)
        val ipHeaderLength = (packet[0].toInt() and 0x0F) * 4
        if (packet.size < ipHeaderLength + UDP_HEADER_SIZE + DNS_HEADER_SIZE) {
            return null
        }

        // Check protocol (should be UDP = 17)
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != UDP_PROTOCOL) return null

        // Get destination port from UDP header
        val udpOffset = ipHeaderLength
        val destPort = ByteBuffer.wrap(packet, udpOffset + 2, 2).short.toInt() and 0xFFFF
        if (destPort != DNS_PORT) return null

        // Parse DNS packet
        val dnsOffset = ipHeaderLength + UDP_HEADER_SIZE
        return extractDnsQuery(packet, dnsOffset)
    }

    /**
     * Extract DNS query information from DNS section of packet.
     */
    private fun extractDnsQuery(data: ByteArray, offset: Int): DnsQuery? {
        if (data.size < offset + DNS_HEADER_SIZE) return null

        // Transaction ID (first 2 bytes of DNS header)
        val transactionId = ByteBuffer.wrap(data, offset, 2).short.toInt() and 0xFFFF

        // Flags (bytes 2-3) - check if it's a query (QR bit = 0)
        val flags = ByteBuffer.wrap(data, offset + 2, 2).short.toInt() and 0xFFFF
        val isQuery = (flags and 0x8000) == 0
        if (!isQuery) return null

        // Question count (bytes 4-5)
        val questionCount = ByteBuffer.wrap(data, offset + 4, 2).short.toInt() and 0xFFFF
        if (questionCount < 1) return null

        // Parse domain name from question section
        val questionOffset = offset + DNS_HEADER_SIZE
        val domainResult = extractDomainName(data, questionOffset) ?: return null
        val (domain, endPos) = domainResult

        // Get query type (2 bytes after domain name)
        if (data.size < endPos + 2) return null
        val queryTypeValue = ByteBuffer.wrap(data, endPos, 2).short.toInt() and 0xFFFF
        val queryType = QueryType.entries.find { it.value == queryTypeValue } ?: QueryType.UNKNOWN

        return DnsQuery(
            transactionId = transactionId,
            domain = domain,
            queryType = queryType
        )
    }

    /**
     * Extract domain name from DNS packet.
     * Returns pair of (domain name, end position in buffer).
     */
    private fun extractDomainName(data: ByteArray, startOffset: Int): Pair<String, Int>? {
        val domain = StringBuilder()
        var pos = startOffset

        while (pos < data.size && data[pos] != 0.toByte()) {
            val labelLength = data[pos].toInt() and 0xFF

            // Check for compression pointer
            if ((labelLength and 0xC0) == 0xC0) {
                // Compression pointer - not fully supported in this basic parser
                pos += 2
                break
            }

            if (labelLength == 0 || pos + labelLength >= data.size) break

            pos++
            for (i in 0 until labelLength) {
                if (pos + i >= data.size) return null
                domain.append(data[pos + i].toInt().toChar())
            }
            pos += labelLength

            if (pos < data.size && data[pos] != 0.toByte()) {
                domain.append('.')
            }
        }

        // Skip the null terminator
        if (pos < data.size && data[pos] == 0.toByte()) {
            pos++
        }

        return if (domain.isNotEmpty()) {
            Pair(domain.toString().lowercase(), pos)
        } else null
    }

    /**
     * Get source IP address from IP packet.
     */
    fun getSourceAddress(packet: ByteArray): String? {
        if (packet.size < 20) return null
        return "${packet[12].toInt() and 0xFF}." +
               "${packet[13].toInt() and 0xFF}." +
               "${packet[14].toInt() and 0xFF}." +
               "${packet[15].toInt() and 0xFF}"
    }

    /**
     * Get destination IP address from IP packet.
     */
    fun getDestAddress(packet: ByteArray): String? {
        if (packet.size < 20) return null
        return "${packet[16].toInt() and 0xFF}." +
               "${packet[17].toInt() and 0xFF}." +
               "${packet[18].toInt() and 0xFF}." +
               "${packet[19].toInt() and 0xFF}"
    }
}
