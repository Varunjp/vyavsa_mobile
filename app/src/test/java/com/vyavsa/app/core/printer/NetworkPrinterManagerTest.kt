package com.vyavsa.app.core.printer

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkPrinterManagerTest {

    private val manager = NetworkPrinterManager()

    @Test
    fun testParseHostWithDefaultPort() {
        val (host, port) = manager.parseHostAndPort("192.168.1.100")
        assertEquals("192.168.1.100", host)
        assertEquals(9100, port)
    }

    @Test
    fun testParseHostWithCustomPort() {
        val (host, port) = manager.parseHostAndPort("192.168.1.100:9200")
        assertEquals("192.168.1.100", host)
        assertEquals(9200, port)
    }

    @Test
    fun testParseHostWithPrefix() {
        val (host, port) = manager.parseHostAndPort("tcp://10.0.0.55:9100")
        assertEquals("10.0.0.55", host)
        assertEquals(9100, port)
    }

    @Test
    fun testParseHostWithWhitespace() {
        val (host, port) = manager.parseHostAndPort("  192.168.1.200:9100  ")
        assertEquals("192.168.1.200", host)
        assertEquals(9100, port)
    }
}
