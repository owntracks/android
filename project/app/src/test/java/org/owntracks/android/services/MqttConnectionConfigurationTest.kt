package org.owntracks.android.services

import org.junit.Assert
import org.junit.Test
import org.owntracks.android.preferences.InMemoryPreferencesStore
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException

class MqttConnectionConfigurationTest {

    @Test
    fun `MQTT Connection Configuration generates correct topics to subscribe to from single default subTopic`() {
        val preferences = Preferences(InMemoryPreferencesStore())
        preferences.subTopic = "owntracks/+/+"
        val topics = preferences.toMqttConnectionConfiguration().topicsToSubscribeTo
        Assert.assertEquals(
            setOf(
                "owntracks/+/+",
                "owntracks/+/+/event",
                "owntracks/+/+/info",
                "owntracks/+/+/waypoints",
                "owntracks/+/+/cmd"
            ),
            topics
        )
    }

    @Test
    fun `MQTT Connection Configuration generates correct topics to subscribe to from single custom subTopic`() {
        val preferences = Preferences(InMemoryPreferencesStore())
        preferences.subTopic = "othertopic/+/+"
        val topics = preferences.toMqttConnectionConfiguration().topicsToSubscribeTo
        Assert.assertEquals(
            setOf("othertopic/+/+"),
            topics
        )
    }

    @Test
    fun `MQTT Connection Configuration generates correct topics to subscribe to from multiple subTopics`() {
        val preferences = Preferences(InMemoryPreferencesStore())
        preferences.subTopic = "owntracks/+/+ othertopic/+"
        preferences.info = true
        val topics = preferences.toMqttConnectionConfiguration().topicsToSubscribeTo
        Assert.assertEquals(
            setOf(
                "owntracks/+/+",
                "othertopic/+"
            ),
            topics
        )
    }

    @Test
    fun `MQTT Connection Configuration generates correct topics to subscribe to from multiple subTopics with info not requested`() {
        val preferences = Preferences(InMemoryPreferencesStore())
        preferences.subTopic = "owntracks/+/+ othertopic/+"
        preferences.info = false
        val topics = preferences.toMqttConnectionConfiguration().topicsToSubscribeTo
        Assert.assertEquals(
            setOf(
                "owntracks/+/+",
                "othertopic/+"
            ),
            topics
        )
    }

    @Test
    fun `MQTT Connection Configuration generates correct topics to subscribe to from wildcard topic`() {
        val preferences = Preferences(InMemoryPreferencesStore())
        preferences.subTopic = "owntracks/#"
        val topics = preferences.toMqttConnectionConfiguration().topicsToSubscribeTo
        Assert.assertEquals(
            setOf("owntracks/#"),
            topics
        )
    }

    @Test(expected = Test.None::class)
    fun `MQTT Connection Configuration validates config with valid hostname`() {
        val preferences = Preferences(InMemoryPreferencesStore())
        preferences.host = "example.com"
        preferences.toMqttConnectionConfiguration()
            .validate()
    }

    @Test(expected = ConfigurationIncompleteException::class)
    fun `MQTT Connection Configuration does not validate config with missing hostname`() {
        val preferences = Preferences(InMemoryPreferencesStore())
        preferences.toMqttConnectionConfiguration()
            .validate()
    }
}
