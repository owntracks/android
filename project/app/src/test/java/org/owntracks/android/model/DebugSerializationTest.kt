package org.owntracks.android.model

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import org.junit.Test
import org.owntracks.android.model.messages.MessageWaypoints

class DebugSerializationTest {
  @OptIn(ExperimentalEncodingApi::class)
  @Test
  fun `debug waypoints round-trip`() {
    val parser = Parser(null)
    val b64 =
        "eyJfdHlwZSI6IndheXBvaW50cyIsIndheXBvaW50cyI6W3siX3R5cGUiOiJ3YXlwb2ludCIsImRlc2MiOiJUZXN0IFdheXBvaW50IiwibGF0Ijo1MSwibG9uIjowLCJyYWQiOjQ1MCwidHN0IjoxNTk4NDUxMzcyfV19"
    val json = String(Base64.decode(b64.toByteArray()))
    println("Input JSON: $json")
    val parsed = parser.fromJson(json)
    println("Parsed: $parsed (type: ${parsed.javaClass.simpleName})")
    if (parsed is MessageWaypoints) {
      println("Waypoints count: ${parsed.waypoints?.size}")
    }
    val reserialised = parser.toJsonPlain(parsed)
    println("Re-serialised: $reserialised")
  }
}
