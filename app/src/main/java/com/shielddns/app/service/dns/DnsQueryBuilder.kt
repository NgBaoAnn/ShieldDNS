package com.shielddns.app.service.dns

import com.shielddns.app.domain.model.DnsQuery
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Builds DNS response packets.
 * Used to create blocked responses (0.0.0.0) or NXDOMAIN responses.
 */
class DnsQueryBuilder {

    companion object {
        private const val DNS_FLAGS_RESPONSE = 0x8180       // Standard response, no error
        private const val DNS_FLAGS_NXDOMAIN = 0x8183       // Name error (NXDOMAIN)
        private const val DNS_TYPE_A = 1                     // A record
        private const val DNS_CLASS_IN = 1                   // Internet class
        private const val DEFAULT_TTL = 300                  // 5 minutes
    }

    /**
     * Build a DNS response that returns 0.0.0.0 for the requested domain.
     * This effectively blocks the domain by returning a null route.
     */
    fun buildBlockedResponse(
        query: DnsQuery,
        originalPacket: ByteArray,
        ipHeaderLength: Int
    ): ByteArray? {
        val dnsOffset = ipHeaderLength + 8 // UDP header is 8 bytes

        // Extract original question section for exact replay
        val questionSection = extractQuestionSection(originalPacket, dnsOffset) ?: return null

        // Build DNS response
        val dnsResponse = buildDnsResponsePayload(
            transactionId = query.transactionId,
            flags = DNS_FLAGS_RESPONSE,
            questionSection = questionSection,
            answerIp = byteArrayOf(0, 0, 0, 0) // 0.0.0.0
        )

        return dnsResponse
    }

    /**
     * Build a DNS NXDOMAIN response indicating the domain doesn't exist.
     */
    fun buildNxdomainResponse(
        query: DnsQuery,
        originalPacket: ByteArray,
        ipHeaderLength: Int
    ): ByteArray? {
        val dnsOffset = ipHeaderLength + 8

        val questionSection = extractQuestionSection(originalPacket, dnsOffset) ?: return null

        // Build DNS NXDOMAIN response (no answer section)
        return buildDnsNxdomainPayload(
            transactionId = query.transactionId,
            questionSection = questionSection
        )
    }

    private fun extractQuestionSection(packet: ByteArray, dnsOffset: Int): ByteArray? {
        if (packet.size < dnsOffset + 12) return null

        // Skip DNS header (12 bytes) to get to question section
        var pos = dnsOffset + 12
        val startPos = pos

        // Skip domain name labels
        while (pos < packet.size && packet[pos] != 0.toByte()) {
            val len = packet[pos].toInt() and 0xFF
            if ((len and 0xC0) == 0xC0) {
                // Compression pointer
                pos += 2
                break
            }
            pos += len + 1
        }

        // Skip null terminator
        if (pos < packet.size && packet[pos] == 0.toByte()) pos++

        // Skip QTYPE and QCLASS (4 bytes)
        pos += 4

        if (pos > packet.size) return null

        return packet.copyOfRange(startPos, pos)
    }

    private fun buildDnsResponsePayload(
        transactionId: Int,
        flags: Int,
        questionSection: ByteArray,
        answerIp: ByteArray
    ): ByteArray {
        val buffer = ByteBuffer.allocate(512).order(ByteOrder.BIG_ENDIAN)

        // DNS Header
        buffer.putShort(transactionId.toShort())  // Transaction ID
        buffer.putShort(flags.toShort())          // Flags
        buffer.putShort(1)                        // Questions: 1
        buffer.putShort(1)                        // Answers: 1
        buffer.putShort(0)                        // Authority RRs: 0
        buffer.putShort(0)                        // Additional RRs: 0

        // Question section (copied from original)
        buffer.put(questionSection)

        // Answer section
        buffer.putShort(0xC00C.toShort())         // Name pointer to question
        buffer.putShort(DNS_TYPE_A.toShort())     // Type: A
        buffer.putShort(DNS_CLASS_IN.toShort())   // Class: IN
        buffer.putInt(DEFAULT_TTL)                // TTL
        buffer.putShort(4)                        // Data length: 4 bytes
        buffer.put(answerIp)                      // IP address

        val result = ByteArray(buffer.position())
        buffer.flip()
        buffer.get(result)
        return result
    }

    private fun buildDnsNxdomainPayload(
        transactionId: Int,
        questionSection: ByteArray
    ): ByteArray {
        val buffer = ByteBuffer.allocate(256).order(ByteOrder.BIG_ENDIAN)

        // DNS Header with NXDOMAIN
        buffer.putShort(transactionId.toShort())
        buffer.putShort(DNS_FLAGS_NXDOMAIN.toShort())
        buffer.putShort(1)  // Questions
        buffer.putShort(0)  // Answers
        buffer.putShort(0)  // Authority
        buffer.putShort(0)  // Additional

        // Question section
        buffer.put(questionSection)

        val result = ByteArray(buffer.position())
        buffer.flip()
        buffer.get(result)
        return result
    }
}
