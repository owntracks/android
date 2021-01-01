package org.owntracks.android.services

import org.junit.Assert
import org.junit.Test

class MessageProcessorEndpointMqttTest {
    @Test
    fun `MQTT Endpoint generates correct topics to subscribe to from single subTopic`() {
        val endpoint = MessageProcessorEndpointMqtt(null, null, null, null, null, null, null)
        val subTopic = "owntracks/+/+"
        val topics = endpoint.getTopicsToSubscribeTo(subTopic, true, "/info", "/events", "/waypoints")
        Assert.assertEquals(setOf("owntracks/+/+", "owntracks/+/+/events", "owntracks/+/+/info", "owntracks/+/+/waypoints"), topics)
    }

    @Test
    fun `MQTT Endpoint generates correct topics to subscribe to from multiple subTopics`() {
        val endpoint = MessageProcessorEndpointMqtt(null, null, null, null, null, null, null)
        val subTopic = "owntracks/+/+ othertopic/+"
        val topics = endpoint.getTopicsToSubscribeTo(subTopic, true, "/info", "/events", "/waypoints")
        Assert.assertEquals(setOf("owntracks/+/+", "owntracks/+/+/events", "owntracks/+/+/info", "owntracks/+/+/waypoints", "othertopic/+", "othertopic/+/events", "othertopic/+/info", "othertopic/+/waypoints"), topics)
    }

    @Test
    fun `MQTT Endpoint generates correct topics to subscribe to from wildcard topic`() {
        val endpoint = MessageProcessorEndpointMqtt(null, null, null, null, null, null, null)
        val subTopic = "owntracks/#"
        val topics = endpoint.getTopicsToSubscribeTo(subTopic, true, "/info", "/events", "/waypoints")
        Assert.assertEquals(setOf("owntracks/#"), topics)
    }
}
