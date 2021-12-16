package org.owntracks.android.testutils

import kotlinx.coroutines.DelicateCoroutinesApi
import mqtt.broker.Broker
import mqtt.packets.MQTTPacket
import org.owntracks.android.model.messages.MessageBase

@ExperimentalUnsignedTypes
interface TestWithAnMQTTBroker {
    fun configureMQTTConnectionToLocal(password: String)
    fun configureMQTTConnectionToLocal()
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