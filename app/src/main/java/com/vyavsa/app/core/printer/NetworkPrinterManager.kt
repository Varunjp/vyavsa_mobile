package com.vyavsa.app.core.printer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages raw TCP socket ESC/POS printing over Ethernet and Wi-Fi local networks (default port 9100).
 */
@Singleton
class NetworkPrinterManager @Inject constructor() {

    /**
     * Parses an IP address or hostname string, supporting optional custom port (e.g. "192.168.1.100:9100").
     * Defaults to the industry standard POS raw socket port 9100 (JetDirect / RAW).
     */
    fun parseHostAndPort(rawAddress: String): Pair<String, Int> {
        val clean = rawAddress
            .trim()
            .removePrefix("tcp://")
            .removePrefix("http://")
            .removePrefix("https://")

        return if (clean.contains(":")) {
            val parts = clean.split(":")
            val host = parts[0].trim()
            val port = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: DEFAULT_PORT
            Pair(host, port)
        } else {
            Pair(clean, DEFAULT_PORT)
        }
    }

    /**
     * Tests TCP socket connectivity to the target network printer without sending any print job.
     */
    suspend fun testConnection(address: String): Result<Unit> = withContext(Dispatchers.IO) {
        val (host, port) = parseHostAndPort(address)
        if (host.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Network printer IP address cannot be blank"))
        }

        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IOException("Cannot connect to printer at $host:$port (${e.message ?: "Connection timed out"})", e))
        }
    }

    /**
     * Connects to the Ethernet / Wi-Fi ESC/POS printer over TCP, writes raw receipt bytes, and cleanly closes socket.
     */
    suspend fun printReceipt(address: String, receiptBytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val (host, port) = parseHostAndPort(address)
        if (host.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Network printer IP address cannot be blank"))
        }

        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
                socket.soTimeout = SO_TIMEOUT_MS
                val out = socket.getOutputStream()
                out.write(receiptBytes)
                out.flush()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IOException("Network printing failed for $host:$port: ${e.message ?: "Transmission error"}", e))
        }
    }

    companion object {
        const val DEFAULT_PORT = 9100
        private const val CONNECT_TIMEOUT_MS = 6000
        private const val SO_TIMEOUT_MS = 10000
    }
}
