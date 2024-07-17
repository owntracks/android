package org.owntracks.android.net.mqtt

import org.junit.Assert
import org.junit.Test
import org.owntracks.android.preferences.InMemoryPreferencesStore
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException
import org.owntracks.android.test.SimpleIdlingResource

class MqttConnectionConfigurationTest {
  private val mockIdlingResource = SimpleIdlingResource("mock", true)

  @Test
  fun `MQTT Connection Configuration generates correct topics to subscribe to from single default subTopic`() {
    val preferences = Preferences(InMemoryPreferencesStore(), mockIdlingResource)
    preferences.subTopic = "owntracks/+/+"
    val topics = preferences.toMqttConnectionConfiguration().topicsToSubscribeTo
    Assert.assertEquals(
        setOf(
            "owntracks/+/+",
            "owntracks/+/+/event",
            "owntracks/+/+/info",
            "owntracks/user/unknown/cmd"),
        topics)
  }

  @Test
  fun `MQTT Connection Configuration generates correct topics to subscribe to from single custom subTopic`() {
    val preferences = Preferences(InMemoryPreferencesStore(), mockIdlingResource)
    preferences.subTopic = "othertopic/+/+"
    val topics = preferences.toMqttConnectionConfiguration().topicsToSubscribeTo
    Assert.assertEquals(setOf("othertopic/+/+"), topics)
  }

  @Test
  fun `MQTT Connection Configuration generates correct topics to subscribe to from multiple subTopics`() {
    val preferences = Preferences(InMemoryPreferencesStore(), mockIdlingResource)
    preferences.subTopic = "owntracks/+/+ othertopic/+"
    preferences.info = true
    val topics = preferences.toMqttConnectionConfiguration().topicsToSubscribeTo
    Assert.assertEquals(setOf("owntracks/+/+", "othertopic/+"), topics)
  }

  @Test
  fun `MQTT Connection Configuration generates correct topics to subscribe to from multiple subTopics with info not requested`() {
    val preferences = Preferences(InMemoryPreferencesStore(), mockIdlingResource)
    preferences.subTopic = "owntracks/+/+ othertopic/+"
    preferences.info = false
    val topics = preferences.toMqttConnectionConfiguration().topicsToSubscribeTo
    Assert.assertEquals(setOf("owntracks/+/+", "othertopic/+"), topics)
  }

  @Test
  fun `MQTT Connection Configuration generates correct topics to subscribe to from wildcard topic`() {
    val preferences = Preferences(InMemoryPreferencesStore(), mockIdlingResource)
    preferences.subTopic = "owntracks/#"
    val topics = preferences.toMqttConnectionConfiguration().topicsToSubscribeTo
    Assert.assertEquals(setOf("owntracks/#"), topics)
  }

  @Test(expected = Test.None::class)
  fun `MQTT Connection Configuration validates config with valid hostname`() {
    val preferences = Preferences(InMemoryPreferencesStore(), mockIdlingResource)
    preferences.host = "example.com"
    preferences.toMqttConnectionConfiguration().validate()
  }

  @Test(expected = ConfigurationIncompleteException::class)
  fun `MQTT Connection Configuration does not validate config with missing hostname`() {
    val preferences = Preferences(InMemoryPreferencesStore(), mockIdlingResource)
    preferences.toMqttConnectionConfiguration().validate()
  }
}
