package org.owntracks.android.model

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.owntracks.android.model.messages.MessageClear
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.preferences.Preferences

class EncryptionProviderTest {
  @Test
  fun `given a preferences with an empty encryptionKey, when encrypting a message then the resulting message is the same as the unencrypted message`() {
    val testPreferences: Preferences = mock { on { encryptionKey } doReturn "" }

    val encryptionProvider = EncryptionProvider(testPreferences)
    val parser = Parser(encryptionProvider)
    val message = MessageClear()
    val encrypted = parser.toJson(message)
    val unencrypted = parser.toJsonPlain(message)
    assertEquals(unencrypted, encrypted)
  }

  @Test
  fun `given a preferences with an simple encryptionKey, when encrypting a message then the resulting message is the same as the unencrypted message`() {
    val testPreferences: Preferences = mock { on { encryptionKey } doReturn "testKey" }

    val encryptionProvider = EncryptionProvider(testPreferences)
    val parser = Parser(encryptionProvider)
    val message =
        MessageLocation().apply {
          latitude = 50.1182933
          longitude = -5.5407733
          accuracy = 5
        }
    val encrypted = parser.toJson(message)

    val decrypted = parser.fromJson(encrypted)
    assertEquals(MessageLocation::class.java, decrypted.javaClass)
    (decrypted as MessageLocation).run { assertEquals(message.messageId, messageId) }
  }

  @Test
  fun `given an encrypted message, when decrypting it then the correct message is returned`() {
    val encryptedMessage =
        """{"_type":"encrypted","_id":"0571e18d","data":"aUiEKyGnskRz383od0mu4uqxTIxeZUddBVxKEmsdv1FFSH2y0NwqLP1tTqOmxaFhGv0bNX1DT7o81FW9Yaxpla6Byvr6CZV3t2skQv8nlxiIv1ZpQWM1bqHE0JFHwD3/yRx9bLHRF3j67kWcO6o2D/xn4ma8Fd4u86NPobFkRIzIhJljphEZyDCGugp7H3FkydGvgEkljb1/P1K91E/ij+m73wFplvTQm8ba5eWSFlQHDdY2B7QwbVPrKarHJLrm1+Y29+HD4Wg6TNhBJZLvdsmek6AcZLB+bq7Q84tUWoLrKp3aiyJZZX70q4HbVI3skkmDWMuMP2cDmsY9bvBjh1ZWAGlOq5VS1dWIlu6OWgWN9VdDbLzP+s2ykeFXrGs="}"""
    val testPreferences: Preferences = mock { on { encryptionKey } doReturn "123" }

    val encryptionProvider = EncryptionProvider(testPreferences)
    val parser = Parser(encryptionProvider)
    val result = parser.fromJson(encryptedMessage)
    assertEquals(MessageLocation::class.java, result.javaClass)
    (result as MessageLocation).run {
      assertEquals(50.1182933, latitude, 0.001)
      assertEquals(-5.5407733, longitude, 0.001)
      assertEquals(5, accuracy)
    }
  }
}
