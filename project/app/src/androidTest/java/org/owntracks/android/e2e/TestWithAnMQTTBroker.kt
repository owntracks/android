package org.owntracks.android.e2e

import kotlinx.coroutines.DelicateCoroutinesApi
import mqtt.broker.Broker
import mqtt.packets.MQTTPacket
import org.owntracks.android.model.messages.MessageBase

interface TestWithAnMQTTBroker {
    fun configureMQTTConnectionToLocal(password: String, timeoutMs: Long)
    fun configureMQTTConnectionToLocal(timeoutMs: Long)
    val mqttPacketsReceived: MutableList<MQTTPacket>
    val broker: Broker
    val mqttUsername: String
    val mqttClientId: String
    val deviceId: String
    fun <E : MessageBase> Collection<E>.sendFromBroker(broker: Broker)
    @DelicateCoroutinesApi
    fun startBroker()
    fun stopBroker()
}