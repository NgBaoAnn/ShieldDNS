package com.shielddns.app.service.tunnel

import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * Manages the TUN interface for VPN packet I/O.
 */
class TunInterface(
    private val vpnInterface: ParcelFileDescriptor
) {
    companion object {
        private const val MAX_PACKET_SIZE = 32767
    }

    private val inputStream: FileInputStream = FileInputStream(vpnInterface.fileDescriptor)
    private val outputStream: FileOutputStream = FileOutputStream(vpnInterface.fileDescriptor)
    private val readBuffer: ByteBuffer = ByteBuffer.allocate(MAX_PACKET_SIZE)

    /**
     * Read a packet from the TUN interface.
     * This is a blocking call.
     * 
     * @return Packet data, or null if no data available
     */
    suspend fun readPacket(): ByteArray? = withContext(Dispatchers.IO) {
        try {
            readBuffer.clear()
            val length = inputStream.read(readBuffer.array())
            if (length > 0) {
                readBuffer.limit(length)
                val packet = ByteArray(length)
                readBuffer.get(packet)
                packet
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Write a packet to the TUN interface.
     */
    suspend fun writePacket(packet: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            outputStream.write(packet)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Close the TUN interface.
     */
    fun close() {
        try {
            inputStream.close()
        } catch (e: Exception) { /* ignore */ }
        
        try {
            outputStream.close()
        } catch (e: Exception) { /* ignore */ }
        
        try {
            vpnInterface.close()
        } catch (e: Exception) { /* ignore */ }
    }
}
