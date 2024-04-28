package org.owntracks.android.services

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.eclipse.paho.client.mqttv3.internal.MqttPersistentData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.net.mqtt.RoomMqttClientPersistence

@SmallTest
@RunWith(AndroidJUnit4::class)
class RoomMqttClientPersistenceTest {
  @Test
  fun mqttClientPersistenceReadWriteWithNullPayloadTest() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    RoomMqttClientPersistence(context, true)
        .apply { open(null, null) }
        .use {
          val key = "testKey"
          val header = "header".toByteArray()
          it.put(key, MqttPersistentData(key, header, 0, header.size, null, 0, 0))
          val result = it.get(key)
          assertNotNull(result)
          assertEquals(6, result.headerLength)
          assertEquals(0, result.payloadLength)
        }
  }

  @Test
  fun mqttClientPersistenceReadWriteWithPayloadTest() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    RoomMqttClientPersistence(context, true)
        .apply { open(null, null) }
        .use {
          val key = "testKey"
          val header = "header".toByteArray()
          val payload = "payload".toByteArray()
          it.put(key, MqttPersistentData(key, header, 0, header.size, payload, 0, payload.size))
          val result = it.get(key)
          assertNotNull(result)
          assertEquals(6, result.headerLength)
          assertEquals(7, result.payloadLength)
        }
  }

  @Test
  fun mqttClientPersistenceClearTest() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    RoomMqttClientPersistence(context, true)
        .apply { open(null, null) }
        .use {
          val key = "testKey"
          val header = "header".toByteArray()
          val payload = "payload".toByteArray()
          it.put(key, MqttPersistentData(key, header, 0, header.size, payload, 0, payload.size))
          it.clear()
          val result = it.keys()
          assertNotNull(result)
          assertEquals(0, result.toList().size)
        }
  }

  @Test
  fun mqttClientPersistenceRemoveTest() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    RoomMqttClientPersistence(context, true)
        .apply { open(null, null) }
        .use {
          val key1 = "testKey1"
          val header1 = "header1".toByteArray()
          val payload1 = "payload1".toByteArray()
          it.put(
              key1, MqttPersistentData(key1, header1, 0, header1.size, payload1, 0, payload1.size))
          val key2 = "testKey2"
          val header2 = "header2".toByteArray()
          val payload2 = "payload2".toByteArray()
          it.put(
              key2, MqttPersistentData(key2, header2, 0, header2.size, payload1, 0, payload2.size))

          it.remove(key1)
          val result = it.keys()
          assertNotNull(result)
          result.toList().run {
            assert(contains(key2))
            assert(!contains(key1))
            assertEquals(1, size)
          }
        }
  }

  @Test
  fun mqttClientPersistenceContainsKeyTest() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    RoomMqttClientPersistence(context, true)
        .apply { open(null, null) }
        .use {
          val key1 = "testKey1"
          val header1 = "header1".toByteArray()
          val payload1 = "payload1".toByteArray()
          it.put(
              key1, MqttPersistentData(key1, header1, 0, header1.size, payload1, 0, payload1.size))
          val key2 = "testKey2"
          val header2 = "header2".toByteArray()
          val payload2 = "payload2".toByteArray()
          it.put(
              key2, MqttPersistentData(key2, header2, 0, header2.size, payload1, 0, payload2.size))

          assert(it.containsKey(key1))
          assert(it.containsKey(key2))
          assert(!it.containsKey("absent"))
        }
  }
}
