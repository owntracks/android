package org.owntracks.android.location.external

/** Simple NMEA 0183 checksum validator (sentences like "$GPGGA,...*5C"). */
internal object NmeaChecksum {
  fun ok(sentence: String): Boolean {
    val asterisk = sentence.indexOf('*')
    if (asterisk < 0 || asterisk + 3 > sentence.length) return false
    val data = sentence.substring(1, asterisk)
    var checksum = 0
    for (c in data) {
      checksum = checksum xor c.code
    }
    val expected = sentence.substring(asterisk + 1, asterisk + 3)
    return try {
      checksum == expected.toInt(16)
    } catch (_: NumberFormatException) {
      false
    }
  }
}
