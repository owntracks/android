package org.owntracks.android.location.external

import android.util.Base64
import java.io.BufferedInputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import timber.log.Timber

/**
 * NTRIP v1 client. Connects to a caster, authenticates with Basic auth, then streams RTCM3
 * corrections to [onRtcmData]. Send the rover's latest GGA via [sendGga] (required for VRS).
 */
internal class NtripClient(
    private val host: String,
    private val port: Int,
    private val mountpoint: String,
    private val user: String,
    private val password: String,
) {
  private companion object {
    const val CONNECT_TIMEOUT_MS = 8_000
    const val READ_TIMEOUT_MS = 15_000
    const val RECONNECT_BASE_MS = 2_000L
    const val RECONNECT_MAX_MS = 30_000L
    const val READ_BUF_SIZE = 4096
    const val GGA_INTERVAL_MS = 10_000L
  }

  var onRtcmData: ((ByteArray, Int) -> Unit)? = null
  var onStatus: ((String) -> Unit)? = null

  val totalBytesReceived = AtomicLong(0L)

  private val running = AtomicBoolean(false)
  private var workerThread: Thread? = null
  private var currentSocket: Socket? = null

  @Volatile private var lastGga: String? = null

  val isConnected: Boolean
    get() = running.get() && currentSocket?.isConnected == true

  fun sendGga(gga: String) {
    lastGga = gga
  }

  fun start() {
    if (!running.compareAndSet(false, true)) return
    workerThread =
        Thread(::connectLoop, "ntrip-client").apply {
          isDaemon = true
          start()
        }
  }

  fun stop() {
    if (!running.compareAndSet(true, false)) return
    closeSocket()
    workerThread?.interrupt()
    workerThread = null
    onStatus?.invoke("NTRIP stopped")
  }

  private fun connectLoop() {
    var backoff = RECONNECT_BASE_MS
    while (running.get()) {
      try {
        onStatus?.invoke("Connecting to $host:$port/$mountpoint")
        doSession()
        backoff = RECONNECT_BASE_MS
      } catch (_: InterruptedException) {
        return
      } catch (e: Exception) {
        if (!running.get()) return
        Timber.w(e, "NTRIP session error")
        onStatus?.invoke("NTRIP error: ${e.message}")
      }
      if (!running.get()) return
      onStatus?.invoke("Reconnecting in ${backoff / 1000}s")
      try {
        Thread.sleep(backoff)
      } catch (_: InterruptedException) {
        return
      }
      backoff = (backoff * 2).coerceAtMost(RECONNECT_MAX_MS)
    }
  }

  private fun doSession() {
    val socket = Socket()
    currentSocket = socket
    try {
      socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
      socket.soTimeout = READ_TIMEOUT_MS

      val out = socket.getOutputStream()
      val auth = Base64.encodeToString("$user:$password".toByteArray(), Base64.NO_WRAP)
      val request = buildString {
        append("GET /$mountpoint HTTP/1.0\r\n")
        append("User-Agent: NTRIP OwnTracks-ExternalGNSS/1.0\r\n")
        append("Authorization: Basic $auth\r\n")
        append("Accept: */*\r\n")
        append("\r\n")
      }
      out.write(request.toByteArray(Charsets.US_ASCII))
      out.flush()

      val inp = BufferedInputStream(socket.getInputStream(), READ_BUF_SIZE)
      val header = readHttpHeader(inp)
      if (!header.startsWith("ICY 200 OK") && !header.contains("200 OK")) {
        val firstLine = header.lineSequence().firstOrNull() ?: header
        onStatus?.invoke("NTRIP rejected: $firstLine")
        return
      }
      onStatus?.invoke("NTRIP connected")

      lastGga?.let { sendGgaToServer(out, it) }

      val buf = ByteArray(READ_BUF_SIZE)
      var lastGgaSentMs = System.currentTimeMillis()
      while (running.get()) {
        val n = inp.read(buf)
        if (n < 0) {
          onStatus?.invoke("NTRIP stream ended")
          break
        }
        if (n > 0) {
          totalBytesReceived.addAndGet(n.toLong())
          onRtcmData?.invoke(buf, n)
        }
        val now = System.currentTimeMillis()
        if (now - lastGgaSentMs >= GGA_INTERVAL_MS) {
          lastGga?.let { sendGgaToServer(out, it) }
          lastGgaSentMs = now
        }
      }
    } finally {
      closeSocket()
    }
  }

  private fun sendGgaToServer(out: OutputStream, gga: String) {
    try {
      val line = if (gga.endsWith("\r\n")) gga else "$gga\r\n"
      out.write(line.toByteArray(Charsets.US_ASCII))
      out.flush()
    } catch (e: Exception) {
      Timber.v(e, "sendGga failed")
    }
  }

  private fun readHttpHeader(inp: BufferedInputStream): String {
    val sb = StringBuilder()
    val maxHeaderBytes = 4096
    while (sb.length < maxHeaderBytes) {
      val b = inp.read()
      if (b < 0) break
      sb.append(b.toChar())
      if (sb.length >= 4 && sb.substring(sb.length - 4) == "\r\n\r\n") break
    }
    return sb.toString()
  }

  private fun closeSocket() {
    runCatching { currentSocket?.close() }
    currentSocket = null
  }
}
