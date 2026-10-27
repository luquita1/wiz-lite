package com.example.wizlite

import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

object WizClient {
    private const val PORT = 38899

    /** Envía un mensaje UDP y espera la respuesta (null si no hay). */
    fun send(ip: String, json: String, timeoutMs: Int = 800): String? {
        DatagramSocket().use { s ->
            s.soTimeout = timeoutMs
            val data = json.toByteArray()
            s.send(DatagramPacket(data, data.size, InetAddress.getByName(ip), PORT))
            val p = DatagramPacket(ByteArray(2048), 2048)
            return try {
                s.receive(p); String(p.data, 0, p.length)
            } catch (e: SocketTimeoutException) { null }
        }
    }

    private fun msg(params: JSONObject) =
        JSONObject().put("method", "setPilot").put("params", params).toString()

    /** Un solo intento, rápido (para vista previa en vivo). */
    fun setPilotOnce(ip: String, params: JSONObject): Boolean =
        try { send(ip, msg(params), 400) != null } catch (_: Exception) { false }

    /** UDP puede perder paquetes: reintenta hasta 3 veces. */
    fun setPilot(ip: String, params: JSONObject): Boolean {
        val m = msg(params)
        repeat(3) {
            try { if (send(ip, m) != null) return true } catch (_: Exception) {}
        }
        return false
    }

    fun getState(ip: String): Boolean? {
        return try {
            val r = send(ip, """{"method":"getPilot"}""") ?: return null
            JSONObject(r).optJSONObject("result")?.optBoolean("state")
        } catch (_: Exception) {
            null
        }
    }

    /** Busca focos WiZ en la red local por broadcast. */
    fun discover(waitMs: Long = 2000): List<String> {
        val found = linkedSetOf<String>()
        try {
            DatagramSocket().use { s ->
                s.broadcast = true
                s.soTimeout = 400
                val data = """{"method":"getPilot"}""".toByteArray()
                s.send(DatagramPacket(data, data.size, InetAddress.getByName("255.255.255.255"), PORT))
                val end = System.currentTimeMillis() + waitMs
                while (System.currentTimeMillis() < end) {
                    val p = DatagramPacket(ByteArray(2048), 2048)
                    try { s.receive(p); p.address.hostAddress?.let { found.add(it) } }
                    catch (_: SocketTimeoutException) {}
                }
            }
        } catch (_: Exception) {}
        return found.toList()
    }
}
