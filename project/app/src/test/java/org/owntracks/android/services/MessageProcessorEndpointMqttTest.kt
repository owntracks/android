package org.owntracks.android.services

import android.content.Context
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert
import org.junit.Test

class MessageProcessorEndpointMqttTest {
    private val applicationContext: Context = mock {
        on { getString(any()) } doReturn "owntracks/+/+"
    }

    @Test
    fun `MQTT Endpoint generates correct topics to subscribe to from single default subTopic`() {
        val endpoint = MessageProcessorEndpointMqtt(null, null, null, null, null, null, applicationContext)
        val subTopic = "owntracks/+/+"
        val topics = endpoint.getTopicsToSubscribeTo(subTopic, true, "/info", "/events", "/waypoints")
        Assert.assertEquals(
                setOf(
                        "owntracks/+/+",
                        "owntracks/+/+/events",
                        "owntracks/+/+/info",
                        "owntracks/+/+/waypoints"
                ), topics)
    }

    @Test
    fun `MQTT Endpoint generates correct topics to subscribe to from single custom subTopic`() {
        val endpoint = MessageProcessorEndpointMqtt(null, null, null, null, null, null, applicationContext)
        val subTopic = "othertopic/+/+"
        val topics = endpoint.getTopicsToSubscribeTo(subTopic, true, "/info", "/events", "/waypoints")
        Assert.assertEquals(
                setOf("othertopic/+/+"), topics)
    }

    @Test
    fun `MQTT Endpoint generates correct topics to subscribe to from multiple subTopics`() {
        val endpoint = MessageProcessorEndpointMqtt(null, null, null, null, null, null, applicationContext)
        val subTopic = "owntracks/+/+ othertopic/+"
        val topics = endpoint.getTopicsToSubscribeTo(subTopic, true, "/info", "/events", "/waypoints")
        Assert.assertEquals(
                setOf(
                        "owntracks/+/+",
                        "othertopic/+"
                ), topics)
    }

    @Test
    fun `MQTT Endpoint generates correct topics to subscribe to from multiple subTopics with info not requested`() {
        val endpoint = MessageProcessorEndpointMqtt(null, null, null, null, null, null, applicationContext)
        val subTopic = "owntracks/+/+ othertopic/+"
        val topics = endpoint.getTopicsToSubscribeTo(subTopic, false, "/info", "/events", "/waypoints")
        Assert.assertEquals(
                setOf(
                        "owntracks/+/+",
                        "othertopic/+",
                ), topics)
    }

    @Test
    fun `MQTT Endpoint generates correct topics to subscribe to from wildcard topic`() {
        val endpoint = MessageProcessorEndpointMqtt(null, null, null, null, null, null, applicationContext)
        val subTopic = "owntracks/#"
        val topics = endpoint.getTopicsToSubscribeTo(subTopic, true, "/info", "/events", "/waypoints")
        Assert.assertEquals(setOf("owntracks/#"), topics)
    }
}
