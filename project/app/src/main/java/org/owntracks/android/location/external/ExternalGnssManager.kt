package org.owntracks.android.location.external

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

/**
 * Manages an external USB GNSS receiver (typically a u-blox F9P).
 *
 * - Opens the USB-serial port.
 * - Sends UBX-CFG-RATE to push it to 10 Hz.
 * - Parses NMEA GGA and forwards fixes via [onFix].
 * - Optionally connects to an NTRIP caster and injects RTCM corrections.
 */
class ExternalGnssManager(
    private val context: Context,
    private val onFix: (lat: Double, lon: Double, alt: Double, accM: Double) -> Unit,
    private val onStatus: (String) -> Unit = {},
    private val onActiveChanged: (Boolean) -> Unit = {},
    @Volatile private var ntripConfig: NtripConfig? = null,
) {
  private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

  private var deviceConnection: UsbDeviceConnection? = null
  private var serialPort: UsbSerialPort? = null
  private var ioManager: SerialInputOutputManager? = null
  private var ioFuture: Future<*>? = null

  private val executor = Executors.newSingleThreadExecutor { r ->
    Thread(r, "external-gnss").apply { isDaemon = true }
  }
  private val started = AtomicBoolean(false)

  private var ntripClient: NtripClient? = null
  private val serialWriteLock = Any()
  private val nmeaBuffer = StringBuilder()

  val isRunning: Boolean
    get() = started.get()

  val isNtripConnected: Boolean
    get() = ntripClient?.isConnected == true

  @Volatile var lastHdop: Double = -1.0
    private set

  val rtcmBytesReceived: Long
    get() = ntripClient?.totalBytesReceived?.get() ?: 0L

  fun start(preferredDevice: UsbDevice? = null) {
    if (!started.compareAndSet(false, true)) {
      Timber.w("External GNSS already started")
      return
    }
    try {
      val driver = findPreferredDriver(preferredDevice)
      if (driver == null) {
        onStatus("No USB-serial device found")
        started.set(false)
        onActiveChanged(false)
        return
      }
      val device = driver.device
      if (!usbManager.hasPermission(device)) {
        onStatus("No USB permission for ${device.deviceName}")
        started.set(false)
        onActiveChanged(false)
        return
      }
      val connection = usbManager.openDevice(device)
      if (connection == null) {
        onStatus("Cannot open USB device")
        started.set(false)
        onActiveChanged(false)
        return
      }
      val port = driver.ports.firstOrNull()
      if (port == null) {
        connection.close()
        onStatus("USB driver exposes no ports")
        started.set(false)
        onActiveChanged(false)
        return
      }

      val baud = ntripConfig?.serialBaud ?: 115200
      port.open(connection)
      port.setParameters(
          baud,
          UsbSerialPort.DATABITS_8,
          UsbSerialPort.STOPBITS_1,
          UsbSerialPort.PARITY_NONE,
      )
      runCatching { port.dtr = true }
      runCatching { port.rts = true }

      deviceConnection = connection
      serialPort = port

      runCatching { UbxConfigSender.configureF9PFor10Hz(port) }

      val listener =
          object : SerialInputOutputManager.Listener {
            override fun onNewData(data: ByteArray) {
              handleIncoming(data)
            }

            override fun onRunError(e: Exception) {
              Timber.w(e, "External GNSS IO error")
              onStatus("External GNSS IO error: ${e.message}")
              stop()
            }
          }
      val manager = SerialInputOutputManager(port, listener)
      ioManager = manager
      ioFuture = executor.submit(manager)

      onStatus("External GNSS started on ${device.deviceName}")
      onActiveChanged(true)

      ntripConfig?.let { startNtripIfConfigured(port, it) }
    } catch (t: Throwable) {
      Timber.e(t, "Unexpected error starting external GNSS")
      onStatus("External GNSS error: ${t.message}")
      started.set(false)
      safeCloseAll()
      onActiveChanged(false)
    }
  }

  fun stop() {
    if (!started.compareAndSet(true, false)) return
    onActiveChanged(false)
    onStatus("External GNSS stopped")
    stopNtrip()
    runCatching { ioManager?.stop() }
    ioManager = null
    runCatching { ioFuture?.cancel(true) }
    ioFuture = null
    safeCloseAll()
  }

  fun updateNtripConfig(config: NtripConfig?) {
    ntripConfig = config
    stopNtrip()
    val port = serialPort
    if (config != null && config.enabled && config.isReady && port != null && started.get()) {
      startNtripWithConfig(config, port)
    }
  }

  // ---------------------------------------------------------------------------
  // NMEA reader
  // ---------------------------------------------------------------------------

  @Synchronized
  private fun handleIncoming(data: ByteArray) {
    for (b in data) {
      val c = (b.toInt() and 0xFF).toChar()
      if (c == '\n') {
        val raw = nmeaBuffer.toString().trimEnd('\r')
        nmeaBuffer.setLength(0)
        if (raw.isNotEmpty()) handleNmeaLine(raw)
      } else if (c != '\u0000') {
        nmeaBuffer.append(c)
        if (nmeaBuffer.length > 512) nmeaBuffer.setLength(0)
      }
    }
  }

  private fun handleNmeaLine(line: String) {
    if (!line.startsWith('$')) return
    if (!NmeaChecksum.ok(line)) return
    val starIdx = line.indexOf('*')
    val noChecksum = if (starIdx > 0) line.substring(0, starIdx) else line
    val parts = noChecksum.split(',')
    if (parts.isEmpty()) return
    if (parts[0].endsWith("GGA", ignoreCase = true)) {
      parseGga(parts)
      ntripClient?.sendGga(line)
    }
  }

  private fun parseGga(fields: List<String>) {
    if (fields.size < 10) return
    val latStr = fields[2]
    val ns = fields[3]
    val lonStr = fields[4]
    val ew = fields[5]
    val fixQuality = fields[6]
    val hdopStr = fields[8]
    val altMslStr = fields[9]
    val geoidSepStr = fields.getOrNull(11)

    if (fixQuality.isEmpty() || fixQuality == "0") return
    if (latStr.isBlank() || lonStr.isBlank()) return
    val lat = nmeaCoordToDeg(latStr, ns)
    val lon = nmeaCoordToDeg(lonStr, ew)
    if (!lat.isFinite() || !lon.isFinite()) return
    val altMsl = altMslStr.toDoubleOrNull() ?: 0.0
    val geoidSep = geoidSepStr?.toDoubleOrNull() ?: 0.0
    val alt = altMsl + geoidSep
    val hdop = hdopStr.toDoubleOrNull() ?: -1.0
    val accM = if (hdop > 0.0) hdop * 1.5 else -1.0
    if (hdop > 0.0) lastHdop = hdop
    onFix(lat, lon, alt, accM)
  }

  private fun nmeaCoordToDeg(coord: String, hemi: String): Double {
    if (coord.length < 4) return Double.NaN
    val dot = coord.indexOf('.')
    val degLen = if (dot > 0) dot - 2 else coord.length - 2
    if (degLen <= 0) return Double.NaN
    val deg = coord.substring(0, degLen).toDoubleOrNull() ?: return Double.NaN
    val min = coord.substring(degLen).toDoubleOrNull() ?: return Double.NaN
    var valDeg = deg + (min / 60.0)
    when (hemi.uppercase()) {
      "S", "W" -> valDeg = -valDeg
    }
    return valDeg
  }

  // ---------------------------------------------------------------------------
  // USB helpers
  // ---------------------------------------------------------------------------

  private fun findPreferredDriver(preferredDevice: UsbDevice?): UsbSerialDriver? {
    val prober = UsbSerialProber.getDefaultProber()
    if (preferredDevice != null) {
      val drv = prober.probeDevice(preferredDevice)
      if (drv != null) return drv
    }
    val drivers = prober.findAllDrivers(usbManager)
    if (drivers.isEmpty()) return null
    val ublox =
        drivers.firstOrNull { d ->
          val p = (d.device.productName ?: "").lowercase()
          val m = runCatching { d.device.manufacturerName ?: "" }.getOrDefault("").lowercase()
          val s = "$p $m"
          (d.device.vendorId == UBLOX_VENDOR_ID) ||
              "u-blox" in s ||
              "ublox" in s ||
              "f9p" in s ||
              "zed" in s
        }
    return ublox ?: drivers.first()
  }

  private fun safeCloseAll() {
    runCatching { serialPort?.close() }
    serialPort = null
    runCatching { deviceConnection?.close() }
    deviceConnection = null
  }

  // ---------------------------------------------------------------------------
  // NTRIP
  // ---------------------------------------------------------------------------

  private fun startNtripIfConfigured(port: UsbSerialPort, cfg: NtripConfig) {
    if (!cfg.enabled || !cfg.isReady) {
      Timber.i("NTRIP not configured; skipping")
      return
    }
    startNtripWithConfig(cfg, port)
  }

  private fun startNtripWithConfig(cfg: NtripConfig, port: UsbSerialPort) {
    val client =
        NtripClient(
            host = cfg.host,
            port = cfg.port,
            mountpoint = cfg.mountpoint,
            user = cfg.user,
            password = cfg.password,
        )
    client.onRtcmData = { data, len ->
      try {
        val chunk = if (len == data.size) data else data.copyOfRange(0, len)
        synchronized(serialWriteLock) { port.write(chunk, 200) }
      } catch (e: Exception) {
        Timber.w(e, "Error writing RTCM to serial")
      }
    }
    client.onStatus = { msg -> onStatus("NTRIP: $msg") }
    ntripClient = client
    client.start()
    onStatus("NTRIP client starting for ${cfg.host}:${cfg.port}/${cfg.mountpoint}")
  }

  private fun stopNtrip() {
    ntripClient?.stop()
    ntripClient = null
  }

  private companion object {
    const val UBLOX_VENDOR_ID = 0x1546
  }
}
