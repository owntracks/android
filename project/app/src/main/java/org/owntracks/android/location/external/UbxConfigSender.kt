package org.owntracks.android.location.external

import com.hoho.android.usbserial.driver.UsbSerialPort
import timber.log.Timber

/**
 * Minimal u-blox UBX frame builder used to put an F9P (or similar) into 10 Hz mode and make sure
 * NMEA GGA is enabled on every port.
 */
internal object UbxConfigSender {
  private const val CLASS_CFG = 0x06
  private const val ID_CFG_RATE = 0x08
  private const val ID_CFG_MSG = 0x01

  fun configureF9PFor10Hz(port: UsbSerialPort) {
    try {
      runCatching { port.purgeHwBuffers(true, true) }

      // CFG-RATE: measRate=100 ms (10 Hz), navRate=1, timeRef=UTC
      val measRateMs = 100
      val navRate = 1
      val timeRef = 0
      val payloadRate =
          byteArrayOf(
              (measRateMs and 0xFF).toByte(),
              ((measRateMs shr 8) and 0xFF).toByte(),
              (navRate and 0xFF).toByte(),
              ((navRate shr 8) and 0xFF).toByte(),
              (timeRef and 0xFF).toByte(),
              ((timeRef shr 8) and 0xFF).toByte(),
          )
      val frameRate = buildUbxFrame(CLASS_CFG, ID_CFG_RATE, payloadRate)
      port.write(frameRate, 500)
      drain(port, "CFG-RATE")

      // CFG-MSG for NMEA GGA (class 0xF0, id 0x00) on every port at rate 1
      val payloadMsg =
          byteArrayOf(
              0xF0.toByte(),
              0x00.toByte(),
              1.toByte(),
              1.toByte(),
              1.toByte(),
              1.toByte(),
              1.toByte(),
              1.toByte(),
          )
      val frameMsg = buildUbxFrame(CLASS_CFG, ID_CFG_MSG, payloadMsg)
      port.write(frameMsg, 500)
      drain(port, "CFG-MSG GGA")
    } catch (e: Exception) {
      Timber.w(e, "Error sending UBX config to F9P")
      throw e
    }
  }

  private fun buildUbxFrame(classId: Int, msgId: Int, payload: ByteArray): ByteArray {
    val len = payload.size
    val frame = ByteArray(6 + len + 2)
    frame[0] = 0xB5.toByte()
    frame[1] = 0x62.toByte()
    frame[2] = classId.toByte()
    frame[3] = msgId.toByte()
    frame[4] = (len and 0xFF).toByte()
    frame[5] = ((len shr 8) and 0xFF).toByte()
    System.arraycopy(payload, 0, frame, 6, len)
    var ckA = 0
    var ckB = 0
    for (i in 2 until 6 + len) {
      val b = frame[i].toInt() and 0xFF
      ckA = (ckA + b) and 0xFF
      ckB = (ckB + ckA) and 0xFF
    }
    frame[6 + len] = ckA.toByte()
    frame[7 + len] = ckB.toByte()
    return frame
  }

  private fun drain(port: UsbSerialPort, label: String) {
    val buf = ByteArray(128)
    runCatching { port.read(buf, 200) }.onFailure { Timber.v(it, "drain %s failed", label) }
  }
}
